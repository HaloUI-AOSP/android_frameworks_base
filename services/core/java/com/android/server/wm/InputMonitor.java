/*
 * Copyright (C) 2023 The Android Open Source Project
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

package com.android.systemui.inputdevice;

import android.util.Log;
import android.view.InputChannel;

/**
 * Monitor for input events from input devices.
 *
 * This class manages input consumers and tracks input device states.
 */
public class InputMonitor {

    private static final String TAG = "InputMonitor";
    private static final boolean DEBUG = true;

    private final InputChannel mInputChannel;
    private final String mName;
    private final OnInputEventListener mListener;

    /** List of registered input consumers - must be cleaned up properly */
    private final ArrayMap<String, InputConsumer> mInputConsumers = new ArrayMap<>();

    /**
     * Callback for input events.
     */
    public interface OnInputEventListener {
        void onInputEvent(InputEvent event);
    }

    /**
     * Creates a new InputMonitor.
     *
     * @param name The name for this monitor
     * @param inputChannel The input channel to monitor
     * @param listener The listener for input events
     */
    public InputMonitor(String name, InputChannel inputChannel, OnInputEventListener listener) {
        mName = name;
        mInputChannel = inputChannel;
        mListener = listener;
    }

    /**
     * Registers an input consumer.
     *
     * @param name The name of the consumer
     * @param channel The input channel for the consumer
     */
    public void registerInputConsumer(String name, InputChannel channel) {
        if (DEBUG) {
            Log.d(TAG, "Registering input consumer: " + name);
        }
        InputConsumer consumer = new InputConsumer(name, channel, mListener);
        mInputConsumers.put(name, consumer);
    }

    /**
     * Unregisters an input consumer.
     *
     * CRITICAL FIX: This method now properly removes the consumer from mInputConsumers
     * in addition to closing the channel. Previously, only the channel was closed without
     * removing from the list, causing the window to persist in memory.
     *
     * @param name The name of the consumer to unregister
     */
    public void unregisterInputConsumer(String name) {
        InputConsumer consumer = mInputConsumers.get(name);
        if (consumer != null) {
            if (DEBUG) {
                Log.d(TAG, "Unregistering input consumer: " + name);
            }
            consumer.close();
            mInputConsumers.remove(name);  // FIX: Remove from list to prevent memory leak
        } else {
            if (DEBUG) {
                Log.w(TAG, "Attempted to unregister non-existent consumer: " + name);
            }
        }
    }

    /**
     * Unregisters all input consumers.
     *
     * @param reason Reason for resetting (for logging)
     */
    public void resetInputConsumers(String reason) {
        if (DEBUG) {
            Log.d(TAG, "Resetting all input consumers, reason: " + reason);
        }

        // Iterate through all consumers and close them
        for (int i = 0; i < mInputConsumers.size(); i++) {
            InputConsumer consumer = mInputConsumers.valueAt(i);
            if (consumer != null) {
                consumer.close();
            }
        }

        // FIX: Clear the entire list to prevent memory leaks
        // Previously this only closed consumers but kept them in the list
        mInputConsumers.clear();
    }

    /**
     * Gets the number of registered input consumers.
     */
    public int getInputConsumerCount() {
        return mInputConsumers.size();
    }

    /**
     * Checks if an input consumer is registered.
     *
     * @param name The name of the consumer
     * @return true if registered
     */
    public boolean hasInputConsumer(String name) {
        return mInputConsumers.containsKey(name);
    }

    /**
     * Gets all registered consumer names (for debugging).
     */
    public String[] getRegisteredConsumerNames() {
        String[] names = new String[mInputConsumers.size()];
        for (int i = 0; i < mInputConsumers.size(); i++) {
            names[i] = mInputConsumers.keyAt(i);
        }
        return names;
    }
}
