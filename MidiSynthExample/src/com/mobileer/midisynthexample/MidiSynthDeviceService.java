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

import android.content.Context;
import android.media.AudioManager;
import android.media.midi.MidiDeviceService;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiReceiver;

import com.mobileer.miditools.synth.LatencyController;
import com.mobileer.miditools.synth.SynthEngine;

public class MidiSynthDeviceService extends MidiDeviceService {
    private static final String TAG = MainActivity.TAG;
    private static SynthEngine mSynthEngine = new SynthEngine();
    private boolean mSynthStarted = false;
    private int mFramesPerBlock;
    private static MidiSynthDeviceService mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
        queryOptimalAudioSettings();
    }

    // Query the system for the best sample rate and buffer size for low latency.
    public void queryOptimalAudioSettings() {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
     //   String text = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_SAMPLE_RATE);
     //   mSampleRate = Integer.parseInt(text);
     //TODO   mSynthEngine.setSampleRate(mSampleRate);
        String text = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
        mFramesPerBlock = 64; //TODO HACK - Integer.parseInt(text);

        mSynthEngine.setFramesPerBlock(mFramesPerBlock);
    }

    @Override
    public void onDestroy() {
        mSynthEngine.stop();
        super.onDestroy();
    }

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] { mSynthEngine };
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
            mSynthEngine.stop();
            mSynthStarted = false;
        }
    }

    public static LatencyController getLatencyController() {
        return mSynthEngine.getLatencyController();
    }

    public static int getMidiByteCount() {
        return mSynthEngine.getMidiByteCount();
    }
}
