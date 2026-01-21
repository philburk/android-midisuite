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

package com.mobileer.example.midiscope;

import android.media.midi.MidiDeviceService;
import android.media.midi.MidiDeviceStatus;
import android.media.midi.MidiReceiver;

import com.mobileer.miditools.MidiFramer;
import com.mobileer.miditools.SmartMidiReceiver;
import com.mobileer.miditools.midi20.inquiry.NegotiatingThread;
import com.mobileer.miditools.midi20.tools.Midi;

import java.io.IOException;

/**
 * Virtual MIDI Device that logs messages to a ScopeLogger.
 */

public class MidiScope extends MidiDeviceService {
    private static final String TAG = "MidiScope";

    private static ScopeLogger mScopeLogger;
    private MyReceiver mInputReceiver = new MyReceiver();
    private static MidiFramer mDeviceFramer;
    private static LoggingReceiver mLoggingReceiver;

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public MidiReceiver[] onGetInputPortReceivers() {
        return new MidiReceiver[] { mInputReceiver };
    }

    public static ScopeLogger getScopeLogger() {
        return mScopeLogger;
    }

    public static void setScopeLogger(ScopeLogger logger) {
        if (logger != null) {
            // Receiver that prints the messages.
            mLoggingReceiver = new LoggingReceiver(logger);
            mDeviceFramer = new MidiFramer(mLoggingReceiver);
        }
        mScopeLogger = logger;
    }

    class MyReceiver extends NegotiatingThread {
        @Override
        public void onSend(byte[] data, int offset, int count,
                long timestamp) throws IOException {
            super.onSend(data, offset, count, timestamp);
            if (mScopeLogger != null) {
                // Send raw data to be parsed into discrete messages.
                if (getNegotiatedVersion() == Midi.VERSION_1_0) {
                    mLoggingReceiver.setUsingPackets(false);
                    mDeviceFramer.send(data, offset, count, timestamp);
                } else {
                    mLoggingReceiver.setUsingPackets(true);
                    mLoggingReceiver.onSend(data, offset, count, timestamp);
                }
            }
        }
    }

    /**
     * This will get called when clients connect or disconnect.
     * Log device information.
     */
    @Override
    public void onDeviceStatusChanged(MidiDeviceStatus status) {
//        mInputReceiver.reset();
        if (mScopeLogger != null) {
            if (status.isInputPortOpen(0)) {
                mScopeLogger.log("=== connected ===");
                String text = MidiPrinter.formatDeviceInfo(
                        status.getDeviceInfo());
                mScopeLogger.log(text);

                if (!NegotiatingThread.isEnabled()) {
                    mScopeLogger.log("CI Negotiation disabled");
                } else {
                    MidiReceiver[] outputPorts = getOutputPortReceivers();
                    if (outputPorts == null) {
                        mScopeLogger.log("Connected device has null output ports.");
                    } else {
                        if (outputPorts.length <= 0) {
                            mScopeLogger.log("Connected device has zero output ports.");
                        } else {
                            if (status.getOutputPortOpenCount(0) <= 0) {
                                mScopeLogger.log("Connected device has no open output ports.");
                            } else {
                                mInputReceiver.setTargetReceiver(getOutputPortReceivers()[0]);
                                // Bidirectional device so try to negotiate.
                                mInputReceiver.start();
                            }
                        }
                    }
                }
            } else {
                mScopeLogger.log("--- disconnected ---");
                mInputReceiver.stop();
            }
        }
    }
}
