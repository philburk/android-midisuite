package com.mobileer.midisynthexample;
/*
 * Copyright (C) 2016 The Android Open Source Project
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

import android.app.Instrumentation;
import android.util.Log;
import android.view.KeyEvent;

import java.util.Timer;
import java.util.TimerTask;

/**
 * Generate fake key events to prevent the CPU speed from being lowered
 * when the user is not touching the screen. Note that this does not work
 * on every Android Device.
 *
 * This can sometimes help if you are using a MIDI keyboard to control a
 * synthesizer running on Android.
 *
 * This class uses the Singleton design pattern because there is no need
 * to have more than one generator and only one View can have focus.
 */
public class FakeKeyGenerator {
    public static final String TAG = "FakeKeyGenerator";
    public static final int FAKE_KEY = KeyEvent.KEYCODE_BACKSLASH;

    private static FakeKeyGenerator mInstance;
    private static Instrumentation instrumentation = new Instrumentation();

    private Timer mFakeKeyTimer;
    private FakeKeyTimerTask mFakeKeyTask;

    static class FakeKeyTimerTask extends TimerTask {
        @Override
        public void run() {
            sendFakeKeyEvent();
        }
    };

    // Prevent direct instantiation of this Singleton.
    private FakeKeyGenerator() {
    }

    public synchronized static FakeKeyGenerator getInstance() {
        if (mInstance == null) {
            mInstance = new FakeKeyGenerator();
        }
        return mInstance;
    }

    /**
     * Post fake key event to keep CPU from throttling down.
     * This should be called at least once per second.
     * It should NOT be called on the UI thread!
     */
    public static void sendFakeKeyEvent() {
        try {
            instrumentation.sendKeyDownUpSync(FAKE_KEY);
        } catch(SecurityException e) {
            // Even though I am honoring window focus, I sometimes get this exception.
            Log.e(TAG, "sendFakeKeyEvent() while out of focus, stopping fake key task");
            getInstance().stop();
        }
    }

    /**
     * Start a timer task that periodically generates a fake key input event.
     * This should be called on the UI thread, usually from onWindowFocusChanged().
     */
    public synchronized void start() {
        stop(); // just in case start() is called twice in a row
        mFakeKeyTimer = new Timer();
        mFakeKeyTask = new FakeKeyTimerTask();
        mFakeKeyTimer.schedule(mFakeKeyTask, 1000, 1000);
    }

    /**
     * Stop the fake key timer task if running.
     * This should be called on the UI thread, usually from onWindowFocusChanged().
     */
    public synchronized void stop() {
        // Cancel the fake key events.
        if (mFakeKeyTimer != null) {
            mFakeKeyTimer.cancel();
            mFakeKeyTimer.purge();
            mFakeKeyTimer = null;
        }
        if (mFakeKeyTask != null){
            mFakeKeyTask.cancel();
            mFakeKeyTask = null;
        }
    }

}
