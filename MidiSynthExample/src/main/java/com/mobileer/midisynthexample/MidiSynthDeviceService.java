/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.mobileer.midisynthexample;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.midi.MidiDeviceService;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiReceiver;

import com.mobileer.miditools.synth.LatencyController;
import com.mobileer.miditools.synth.SynthEngine;

public class MidiSynthDeviceService extends MidiDeviceService {
    private static final String TAG = MainActivity.TAG;
    private static final int ONGOING_NOTIFICATION_ID = 1793;
    private static SynthEngine mSynthEngine = new SynthEngine();
    private boolean mSynthStarted = false;
    private static MidiSynthDeviceService mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;

        startForegroundService();
        queryOptimalAudioSettings();
    }

    // Query the system for the best sample rate and buffer size for low latency.
    public void queryOptimalAudioSettings() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        String text = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        int framesPerBlock = Integer.parseInt(text);
        mSynthEngine.setFramesPerBlock(framesPerBlock);
    }

    @Override
    public void onDestroy() {
        mSynthEngine.stop();
        stopForegroundService();
        super.onDestroy();
    }

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] { mSynthEngine };
    }

    private void startForegroundService() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent =
                PendingIntent.getActivity(this, 0, notificationIntent, 0);

        Notification notification =
                //new Notification.Builder(this, CHANNEL_DEFAULT_IMPORTANCE)
                new Notification.Builder(this)
                        .setContentTitle(getText(R.string.notification_title))
                        .setContentText(getText(R.string.notification_message))
                        .setSmallIcon(R.drawable.icon)
                        .setContentIntent(pendingIntent)
                        .setTicker(getText(R.string.ticker_text))
                        .build();

        startForeground(ONGOING_NOTIFICATION_ID, notification);
    }

    private void stopForegroundService() {
        stopForeground(true);
    }

    /**
     * This will get called when clients connect or disconnect.
     */
    @Override
    public void onDeviceStatusChanged(MidiDeviceStatus status) {
        if (status.isInputPortOpen(0) && !mSynthStarted) {
            mSynthEngine.start();
            mSynthStarted = true;
        } else if (!status.isInputPortOpen(0) && mSynthStarted){
            mSynthStarted = false;
            mSynthEngine.stop();
        }
    }

    public static LatencyController getLatencyController() {
        return mSynthEngine.getLatencyController();
    }

    public static int getMidiByteCount() {
        return mSynthEngine.getMidiByteCount();
    }
}
