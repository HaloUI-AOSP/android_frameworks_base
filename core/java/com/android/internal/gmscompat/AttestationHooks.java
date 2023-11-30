/*
 * Copyright (C) 2021 The Android Open Source Project
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

package com.android.internal.gmscompat;

import android.app.Application;
import android.os.Build;
import android.content.Context;
import android.os.SystemProperties;
import android.util.Log;

import java.lang.reflect.Field;
import java.util.Arrays;

/** @hide */
public final class AttestationHooks {
    private static final String TAG = "GmsCompat/Attestation";
    private static final String PACKAGE_GMS = "com.google.android.gms";
    private static final String PACKAGE_FINSKY = "com.android.vending";
    private static final String PROCESS_UNSTABLE = "com.google.android.gms.unstable";

    private static volatile boolean sIsGms = false;
    private static volatile boolean sIsFinsky = false;

    private AttestationHooks() { }

    private static void setBuildField(String key, String value) {
        try {
            // Unlock
            Field field = Build.class.getDeclaredField(key);
            field.setAccessible(true);

            // Edit
            field.set(null, value);

            // Lock
            field.setAccessible(false);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            Log.e(TAG, "Failed to spoof Build." + key, e);
        }
    }

    private static void spoofBuildGms() {
        // Alter model name and fingerprint to avoid hardware attestation enforcement
        setBuildField("DEVICE", "bullhead");
        setBuildField("FINGERPRINT", "google/bullhead/bullhead:8.0.0/OPR6.170623.013/4283548:user/release-keys");
        setBuildField("MODEL", "Nexus 5X");
        setBuildField("PRODUCT", "bullhead");
    }

    public static void initApplicationBeforeOnCreate(Context context) {
        final String packageName = context.getPackageName();
        final String processName = Application.getProcessName();

        if (TextUtils.isEmpty(packageName) || TextUtils.isEmpty(processName)) {
            Log.e(TAG, "Null package or process name");
            return;
        }

        if (PACKAGE_GMS.equals(packageName) &&
                PROCESS_UNSTABLE.equals(processName())) {
            sIsGms = true;
            spoofBuildGms();
        }

        if (PACKAGE_FINSKY.equals(packageName)) {
            sIsFinsky = true;
        }
    }

    private static boolean isCallerSafetyNet() {
        return sIsGms && Arrays.stream(Thread.currentThread().getStackTrace())
                .anyMatch(elem -> elem.getClassName().contains("DroidGuard"));
    }

    public static void onEngineGetCertificateChain() {
        // Check stack for SafetyNet or Play Integrity
        if (isCallerSafetyNet() || sIsFinsky) {
            Log.i(TAG, "Blocked key attestation sIsGms=" + sIsGms + " sIsFinsky=" + sIsFinsky);
            throw new UnsupportedOperationException();
        }
    }
}
