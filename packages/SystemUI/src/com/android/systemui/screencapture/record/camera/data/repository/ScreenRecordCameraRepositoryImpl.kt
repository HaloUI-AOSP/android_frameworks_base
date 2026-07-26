/*
 * Copyright (C) 2025 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.systemui.screencapture.record.camera.data.repository

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.ImageFormat
import android.graphics.Region
import android.graphics.SurfaceTexture
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCaptureSession
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraDevice
import android.hardware.camera2.CameraManager
import android.os.Handler
import android.util.Log
import android.util.Size
import android.view.Surface
import com.android.systemui.dagger.SysUISingleton
import com.android.systemui.dagger.qualifiers.Application
import com.android.systemui.dagger.qualifiers.Background
import com.android.systemui.screencapture.record.camera.data.model.StreamConfiguration
import com.android.systemui.screencapture.record.camera.shared.model.CameraState
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

private const val TAG = "ScreenRecordCameraRepo"

@SysUISingleton
class ScreenRecordCameraRepositoryImpl
@Inject
constructor(
    @Application private val context: Context,
    @Background private val bgCoroutineContext: CoroutineContext,
    @Background private val bgHandler: Handler,
    @Application private val applicationScope: CoroutineScope,
) : ScreenRecordCameraRepository {

    private val cameraManager: CameraManager? by lazy {
        context.getSystemService(CameraManager::class.java)
    }

    private val _errors = MutableSharedFlow<Int>(extraBufferCapacity = 16)
    override val errors: Flow<Int> = _errors.asSharedFlow()

    private val _state = MutableStateFlow(CameraState.Stopped)
    override val state: StateFlow<CameraState> = _state.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    override val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    override val cameraSubjectBounds: StateFlow<Region?> = MutableStateFlow(null).asStateFlow()
    override val isBackgroundColorAvailable: StateFlow<Boolean> =
        MutableStateFlow(false).asStateFlow()

    private val mutex = Mutex()
    private var cameraDevice: CameraDevice? = null
    private var captureSession: CameraCaptureSession? = null
    private var isStopRequested = false

    init {
        applicationScope.launch(bgCoroutineContext) { connectInternal() }
    }

    override fun connect() {
        applicationScope.launch(bgCoroutineContext) { connectInternal() }
    }

    private suspend fun connectInternal() {
        val hasFrontCamera = getFrontCameraId() != null
        _isConnected.value = hasFrontCamera
    }

    override fun disconnect() {
        applicationScope.launch(bgCoroutineContext) {
            _isConnected.value = false
            stopStream()
        }
    }

    override suspend fun isCameraSupported(): Boolean =
        withContext(bgCoroutineContext) { getFrontCameraId() != null }

    override suspend fun isOnTapSupported(): Boolean = false

    override suspend fun isBackgroundColorSupported(): Boolean = false

    override suspend fun prepareStream(
        displayUniqueId: String?,
        @Surface.Rotation displayRotation: Int,
    ): StreamConfiguration? =
        withContext(bgCoroutineContext) {
            val cameraId = getFrontCameraId() ?: return@withContext null
            val manager = cameraManager ?: return@withContext null
            try {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val sensorOrientation =
                    characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION) ?: 270

                val displayDegrees =
                    when (displayRotation) {
                        Surface.ROTATION_0 -> 0
                        Surface.ROTATION_90 -> 90
                        Surface.ROTATION_180 -> 180
                        Surface.ROTATION_270 -> 270
                        else -> 0
                    }

                val totalRotation = (sensorOrientation + displayDegrees) % 360
                val isRotated = totalRotation == 90 || totalRotation == 270

                val map =
                    characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
                        ?: return@withContext null
                val sizes =
                    map.getOutputSizes(SurfaceTexture::class.java)
                        ?: map.getOutputSizes(ImageFormat.YUV_420_888)
                        ?: return@withContext null

                if (sizes.isEmpty()) return@withContext null

                val targetArea = 640 * 480
                val chosenSize =
                    sizes.minByOrNull { size ->
                        val aspectErr =
                            Math.abs((size.width.toDouble() / size.height) - (4.0 / 3.0))
                        val areaErr = Math.abs(size.width * size.height - targetArea)
                        aspectErr * 10000 + areaErr
                    } ?: sizes[0]

                val outputStreamSize =
                    if (isRotated) {
                        Size(chosenSize.height, chosenSize.width)
                    } else {
                        chosenSize
                    }

                StreamConfiguration(
                    cameraStreamSize = chosenSize,
                    outputStreamSize = outputStreamSize,
                )
            } catch (e: CameraAccessException) {
                Log.e(TAG, "CameraAccessException preparing stream", e)
                _errors.tryEmit(e.reason)
                null
            } catch (e: Exception) {
                Log.e(TAG, "Error preparing camera stream", e)
                null
            }
        }

    override suspend fun startStream(surface: Surface, size: Size): Unit =
        withContext(bgCoroutineContext) {
            mutex.withLock {
                if (_state.value == CameraState.Starting || _state.value == CameraState.Started) {
                    Log.w(
                        TAG,
                        "startStream called while state is ${_state.value}, stopping previous stream first",
                    )
                    internalStopStreamLocked()
                }

                if (
                    context.checkSelfPermission(Manifest.permission.CAMERA) !=
                        PackageManager.PERMISSION_GRANTED
                ) {
                    Log.e(TAG, "CAMERA permission not granted")
                    _errors.tryEmit(CameraDevice.StateCallback.ERROR_CAMERA_DISABLED)
                    _state.value = CameraState.Stopped
                    return@withContext
                }

                val cameraId = getFrontCameraId()
                if (cameraId == null) {
                    Log.e(TAG, "No front camera found")
                    _state.value = CameraState.Stopped
                    return@withContext
                }

                val manager = cameraManager
                if (manager == null) {
                    Log.e(TAG, "CameraManager unavailable")
                    _state.value = CameraState.Stopped
                    return@withContext
                }

                _state.value = CameraState.Starting
                isStopRequested = false

                try {
                    manager.openCamera(
                        cameraId,
                        object : CameraDevice.StateCallback() {
                            override fun onOpened(camera: CameraDevice) {
                                applicationScope.launch(bgCoroutineContext) {
                                    mutex.withLock {
                                        if (isStopRequested) {
                                            Log.i(
                                                TAG,
                                                "Stop was requested while camera was opening; closing camera",
                                            )
                                            camera.close()
                                            _state.value = CameraState.Stopped
                                            return@withLock
                                        }
                                        cameraDevice = camera
                                        createCaptureSessionLocked(camera, surface)
                                    }
                                }
                            }

                            override fun onDisconnected(camera: CameraDevice) {
                                applicationScope.launch(bgCoroutineContext) {
                                    mutex.withLock {
                                        Log.w(TAG, "Camera disconnected")
                                        cleanupCameraLocked(camera)
                                        _state.value = CameraState.Stopped
                                    }
                                }
                            }

                            override fun onError(camera: CameraDevice, error: Int) {
                                applicationScope.launch(bgCoroutineContext) {
                                    mutex.withLock {
                                        Log.e(TAG, "Camera device error: $error")
                                        _errors.tryEmit(error)
                                        cleanupCameraLocked(camera)
                                        _state.value = CameraState.Stopped
                                    }
                                }
                            }
                        },
                        bgHandler,
                    )
                } catch (e: CameraAccessException) {
                    Log.e(TAG, "CameraAccessException opening camera", e)
                    _errors.tryEmit(e.reason)
                    _state.value = CameraState.Stopped
                } catch (e: SecurityException) {
                    Log.e(TAG, "Missing camera permission to start stream", e)
                    _errors.tryEmit(CameraDevice.StateCallback.ERROR_CAMERA_DISABLED)
                    _state.value = CameraState.Stopped
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to start camera stream", e)
                    _state.value = CameraState.Stopped
                }
            }
        }

    private fun createCaptureSessionLocked(camera: CameraDevice, surface: Surface) {
        try {
            camera.createCaptureSession(
                listOf(surface),
                object : CameraCaptureSession.StateCallback() {
                    override fun onConfigured(session: CameraCaptureSession) {
                        applicationScope.launch(bgCoroutineContext) {
                            mutex.withLock {
                                if (isStopRequested) {
                                    session.close()
                                    cleanupCameraLocked(camera)
                                    _state.value = CameraState.Stopped
                                    return@withLock
                                }
                                captureSession = session
                                try {
                                    val requestBuilder =
                                        camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW)
                                    requestBuilder.addTarget(surface)
                                    session.setRepeatingRequest(
                                        requestBuilder.build(),
                                        null,
                                        bgHandler,
                                    )
                                    _state.value = CameraState.Started
                                } catch (e: Exception) {
                                    Log.e(TAG, "Failed to start repeating request", e)
                                    cleanupCameraLocked(camera)
                                    _state.value = CameraState.Stopped
                                }
                            }
                        }
                    }

                    override fun onConfigureFailed(session: CameraCaptureSession) {
                        applicationScope.launch(bgCoroutineContext) {
                            mutex.withLock {
                                Log.e(TAG, "Capture session configuration failed")
                                session.close()
                                cleanupCameraLocked(camera)
                                _state.value = CameraState.Stopped
                            }
                        }
                    }
                },
                bgHandler,
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create capture session", e)
            cleanupCameraLocked(camera)
            _state.value = CameraState.Stopped
        }
    }

    override suspend fun stopStream(): Unit =
        withContext(bgCoroutineContext) { mutex.withLock { internalStopStreamLocked() } }

    private fun internalStopStreamLocked() {
        isStopRequested = true
        _state.value = CameraState.Stopping
        try {
            captureSession?.close()
            captureSession = null
            cameraDevice?.close()
            cameraDevice = null
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping camera stream", e)
        } finally {
            _state.value = CameraState.Stopped
        }
    }

    private fun cleanupCameraLocked(camera: CameraDevice?) {
        try {
            captureSession?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing captureSession during cleanup", e)
        }
        captureSession = null

        try {
            camera?.close() ?: cameraDevice?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing cameraDevice during cleanup", e)
        }
        cameraDevice = null
    }

    override suspend fun setBackgroundColor(color: Int) {}

    override suspend fun onTap() {}

    private fun getFrontCameraId(): String? {
        val manager = cameraManager ?: return null
        try {
            for (cameraId in manager.cameraIdList) {
                val characteristics = manager.getCameraCharacteristics(cameraId)
                val facing = characteristics.get(CameraCharacteristics.LENS_FACING)
                if (facing == CameraCharacteristics.LENS_FACING_FRONT) {
                    return cameraId
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error finding front camera", e)
        }
        return null
    }
}
