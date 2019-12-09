/*
 * Copyright (c) 2015 The CyanogenMod Project
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

package com.custom.ambient.display;

import android.app.ActivityManager;
import android.app.ActivityManager.RunningServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.hardware.display.AmbientDisplayConfiguration;
import android.os.UserHandle;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;

public final class Utils {

    private static final String TAG = "DozeUtils";
    private static final boolean DEBUG = false;

    private static final String DOZE_INTENT = "com.android.systemui.doze.pulse";

    protected static final String AOD_KEY = "always_on_ambient";
    protected static final String DOZE_ON_CHARGE = "doze_on_charge";
    protected static final String AMBIENT_DISPLAY_KEY = "ambient_display";
    protected static final String PICK_UP_KEY = "pick_up";
    protected static final String GESTURE_HAND_WAVE_KEY = "gesture_hand_wave";
    protected static final String GESTURE_POCKET_KEY = "gesture_pocket";
    protected static final String DOUBLE_TAP_KEY = "doze_trigger_doubletap";
    protected static final String MUSIC_TICKER_KEY = "pulse_on_new_tracks";
    protected static final String OMNI_DOZE_BRIGHTNESS_KEY = "doze_brightness";
    protected static final String OMNI_PULSE_BRIGHTNESS_KEY = "pulse_brightness";

    protected static void startService(Context context) {
        if (DEBUG) Log.d(TAG, "Starting service");
        if (!isServiceRunning(DozeService.class, context)) {
            context.startService(new Intent(context, DozeService.class));
        }
    }

    private static boolean isServiceRunning(Class<?> serviceClass, Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (manager != null) {
            for (RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
                if (serviceClass.getName().equals(service.service.getClassName())) {
                    return true;
                }
            }
        }
        return false;
    }

    protected static void stopService(Context context) {
        if (DEBUG) Log.d(TAG, "Stopping service");
        context.stopService(new Intent(context, DozeService.class));
    }

    protected static boolean isAoDEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON, 0) != 0;
    }

    protected static boolean isAoDAvailable(Context context) {
        final AmbientDisplayConfiguration config = new AmbientDisplayConfiguration(context);
        return config.alwaysOnAvailable();
    }

    protected static boolean enableAoD(boolean enable, Context context) {
        boolean enabled = Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.DOZE_ALWAYS_ON, enable ? 1 : 0);
        if (enable) {
            enablePickUp(false, context);
            enableHandWave(false, context);
            enablePocketMode(false, context);
        }

        return enabled;
    }

    protected static boolean isDozeEnabled(Context context) {
        return Settings.Secure.getInt(context.getContentResolver(),
                Settings.Secure.DOZE_ENABLED, 1) != 0;
    }

    protected static boolean dozeOnChargeEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.DOZE_ON_CHARGE, 0) != 0;
    }

    protected static boolean tiltGestureEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.CUSTOM_AMBIENT_TILT_GESTURE, 0) != 0;
    }

    protected static boolean handwaveGestureEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.CUSTOM_AMBIENT_HANDWAVE_GESTURE, 0) != 0;
    }

    protected static boolean pocketGestureEnabled(Context context) {
        return Settings.System.getInt(context.getContentResolver(),
                Settings.System.CUSTOM_AMBIENT_POCKETMODE_GESTURE, 0) != 0;
    }

    protected static boolean enableDoze(boolean enable, Context context) {
        boolean enabled = Settings.Secure.putInt(context.getContentResolver(),
                Settings.Secure.DOZE_ENABLED, enable ? 1 : 0);
        // don't start the service, for notifications pulse we don't need the proximity sensor check here
        return enabled;
    }

    protected static boolean enableDozeOnCharge(boolean enable, Context context) {
        boolean enabled = Settings.System.putInt(context.getContentResolver(),
                Settings.System.DOZE_ON_CHARGE, enable ? 1 : 0);
        manageService(context);
        return enabled;
    }

    protected static boolean enablePickUp(boolean enable, Context context) {
        // shared pref value already updated by DozeSettings.onPreferenceChange
        boolean enabled = Settings.System.putInt(context.getContentResolver(),
                Settings.System.CUSTOM_AMBIENT_TILT_GESTURE, enable ? 1 : 0);
        manageService(context);
        return enabled;
    }

    protected static boolean enableHandWave(boolean enable, Context context) {
        boolean enabled = Settings.System.putInt(context.getContentResolver(),
                Settings.System.CUSTOM_AMBIENT_HANDWAVE_GESTURE, enable ? 1 : 0);
        manageService(context);
        return enabled;
    }

    protected static boolean enablePocketMode(boolean enable, Context context) {
        boolean enabled = Settings.System.putInt(context.getContentResolver(),
                Settings.System.CUSTOM_AMBIENT_POCKETMODE_GESTURE, enable ? 1 : 0);
        manageService(context);
        return enabled;
    }

    private static void manageService(Context context) {
        if (sensorsEnabled(context)) {
            startService(context);
        } else {
            stopService(context);
        }
    }

    protected static void launchDozePulse(Context context) {
        if (DEBUG) Log.d(TAG, "Launch doze pulse");
        context.sendBroadcastAsUser(new Intent(DOZE_INTENT),
                new UserHandle(UserHandle.USER_CURRENT));
    }

    protected static boolean sensorsEnabled(Context context) {
        return tiltGestureEnabled(context) || handwaveGestureEnabled(context)
                || pocketGestureEnabled(context);
    }

    protected static Sensor getSensor(SensorManager sm, String type) {
        for (Sensor sensor : sm.getSensorList(Sensor.TYPE_ALL)) {
            if (type.equals(sensor.getStringType())) {
                return sensor;
            }
        }
        return null;
    }
}
