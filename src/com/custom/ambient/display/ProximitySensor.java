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

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class ProximitySensor implements SensorEventListener {

    private static final boolean DEBUG = false;
    private static final String TAG = "ProximitySensor";

    private static final int POCKET_DELTA_NS = 1000 * 1000 * 1000;

    private SensorManager mSensorManager;
    private Sensor mSensor;
    private Context mContext;

    private boolean mSawNear = false;
    private long mInPocketTime = 0;

    private boolean mHandwaveGestureEnabled;
    private boolean mPocketGestureEnabled;

    private final ExecutorService mExecutorService;

    public ProximitySensor(Context context) {
        mContext = context;
        mSensorManager = (SensorManager)
                mContext.getSystemService(Context.SENSOR_SERVICE);
        if (mSensorManager != null) {
            String customProximity = mContext.getResources().getString(
                                        R.string.config_custom_proximity);
            if (!customProximity.isEmpty()) {
                mSensorManager = mContext.getSystemService(SensorManager.class);
                mSensor = Utils.getSensor(mSensorManager, customProximity);
            } else {
                mSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY);
            }
        }
        mExecutorService = Executors.newSingleThreadExecutor();
    }

    private Future<?> submit(Runnable runnable) {
        return mExecutorService.submit(runnable);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        boolean isNear = event.values[0] < mSensor.getMaximumRange();
        if (mSawNear && !isNear) {
            if (shouldPulse(event.timestamp)) {
                Utils.launchDozePulse(mContext);
            }
        } else {
            mInPocketTime = event.timestamp;
        }
        mSawNear = isNear;
    }

    private boolean shouldPulse(long timestamp) {
        long delta = timestamp - mInPocketTime;

        if (mHandwaveGestureEnabled && mPocketGestureEnabled) {
            return true;
        } else if (mHandwaveGestureEnabled && !mPocketGestureEnabled) {
            return delta < POCKET_DELTA_NS;
        } else if (!mHandwaveGestureEnabled && mPocketGestureEnabled) {
            return delta >= POCKET_DELTA_NS;
        }
        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        /* Empty */
    }

    // Switching screen OFF - we enable the sensor
    protected void enable() {
        if (DEBUG) Log.d(TAG, "Enabling");
        submit(() -> {
            // We save user settings so at next screen ON call (enable())
            // we don't need to read them again from the Settings provider
            mHandwaveGestureEnabled = Utils.handwaveGestureEnabled(mContext);
            mPocketGestureEnabled = Utils.pocketGestureEnabled(mContext);
            if (mHandwaveGestureEnabled || mPocketGestureEnabled) {
                mSensorManager.registerListener(this, mSensor,
                        SensorManager.SENSOR_DELAY_NORMAL);
            }
        });
    }

    // Switching screen ON - we disable the sensor
    protected void disable() {
        if (DEBUG) Log.d(TAG, "Disabling");
        submit(() -> {
            if (mHandwaveGestureEnabled || mPocketGestureEnabled) {
                mSensorManager.unregisterListener(this, mSensor);
            }
        });
    }
}
