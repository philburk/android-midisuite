/*
 * Copyright (C) 2014 The Android Open Source Project
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

package com.android.midisynthexample;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice;
import android.media.midi.MidiDevice.MidiConnection;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiInputPort;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.miditools.MidiEventThread;
import com.android.miditools.MidiInputPortSelector;
import com.android.miditools.MidiOutputPortConnectionSelector;
import com.android.miditools.MidiOutputPortSelector;
import com.android.miditools.MidiPortConnector;
import com.android.miditools.MidiPortSelector;
import com.android.miditools.MidiPortWrapper;
import com.android.miditools.MidiTools;

import java.io.IOException;
import java.util.LinkedList;

/**
 * Simple synthesizer as a MIDI Device.
 */
public class MainActivity extends Activity {
    static final String TAG = "MidiSynthExample";

    private MidiManager mMidiManager;
    private MidiOutputPortConnectionSelector mPortSelector;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(MainActivity.this,
                    "MIDI not supported!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    private void setupMidi() {
        // Setup MIDI
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        MidiDeviceInfo synthInfo =  MidiTools.findDevice(mMidiManager, "AndroidTest",
                "SynthExample");
        int portIndex = 0;
        mPortSelector = new MidiOutputPortConnectionSelector(mMidiManager, this,
                R.id.spinner_synth_sender, synthInfo, portIndex);
        mPortSelector.setConnectedListener(new MyPortsConnectedListener());
    }

    private void closeSynthResources() {
        if (mPortSelector != null) {
            mPortSelector.close();
        }
    }

    // TODO Listen to the synth server
    // for open/close events and then disable/enable the spinner.
    private class MyPortsConnectedListener
            implements MidiPortConnector.OnPortsConnectedListener {
        @Override
        public void onPortsConnected(final MidiConnection connection) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (connection == null) {
                        Toast.makeText(MainActivity.this,
                                R.string.error_port_busy, Toast.LENGTH_LONG)
                                .show();
                        mPortSelector.clearSelection();
                    } else {
                        Toast.makeText(MainActivity.this,
                                R.string.port_open_ok, Toast.LENGTH_LONG)
                                .show();
                    }
                }
            });
        }
    }


    public void onToggleScreenLock(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        if (checked) {
            getWindow()
                    .addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        } else {
            getWindow()
                    .clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }
    }

    @Override
    public void onDestroy() {
        closeSynthResources();
        super.onDestroy();
    }

}
