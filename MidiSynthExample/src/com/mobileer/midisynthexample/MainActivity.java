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

package com.mobileer.midisynthexample;

import android.app.Activity;
import android.app.Fragment;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.media.midi.MidiDevice.MidiConnection;
import android.media.midi.MidiDeviceInfo;
import android.media.midi.MidiManager;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mobileer.miditools.MidiDeviceMonitor;
import com.mobileer.miditools.MidiOutputPortConnectionSelector;
import com.mobileer.miditools.MidiPortConnector;
import com.mobileer.miditools.MidiTools;
import com.mobileer.miditools.synth.LatencyController;

/**
 * Simple synthesizer as a MIDI Device.
 */
public class MainActivity extends Activity {
    static final String TAG = "MidiSynthExample";

    private MidiManager mMidiManager;
    private MidiOutputPortConnectionSelector mPortSelector;
    private LatencyController mLatencyController;
    private TextView mLatencyLog;
    private LinearLayout mLatencyLayout;
    private Handler mLatencyHandler;
    private CheckBox mLatencyCheckBox;
    private CheckBox mOptimizeSizeCheckBox;

    private Runnable mLatencyRunnable = new Runnable() {
        @Override
        public void run() {
            updateStatusView();
            // Repeat several times per second.
            mLatencyHandler.postDelayed(mLatencyRunnable, 1000 / 5);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.main);

        mLatencyCheckBox = (CheckBox) findViewById(R.id.checkbox_low_latency);
        mOptimizeSizeCheckBox = (CheckBox) findViewById(R.id.checkbox_optimize);
        mLatencyLog = (TextView) findViewById(R.id.text_latency);
        mLatencyLayout = (LinearLayout) findViewById(R.id.layout_latency);

        mLatencyController = MidiSynthDeviceService.getLatencyController();
        if (mLatencyController.isLowLatencySupported()) {
            // Start out with low latency.
            mLatencyCheckBox.setChecked(true);
            mLatencyController.setLowLatencyEnabled(true);
            mOptimizeSizeCheckBox.setChecked(true);
            mLatencyController.setAutoSizeEnabled(true);
        } else {
            mLatencyLayout.setVisibility(View.GONE);
        }

        // Create the Handler object (on the main thread by default)
        mLatencyHandler = new Handler();

        // Is Android MIDI supported?
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(MainActivity.this,
                    "MIDI not supported!", Toast.LENGTH_LONG)
                    .show();
        }
    }

    // Display information about the audio output latency.
    private void updateStatusView() {
        String text = "";
        if (mLatencyController.isRunning()){
            text = "Buffering " + mLatencyController.getBufferSizeInFrames()
                    + " of "
                    + mLatencyController.getBufferCapacityInFrames() + " frames.\n";
            text += "Underruns = " + mLatencyController.getUnderrunCount() + "\n";
            text += "Single CPU load = " + mLatencyController.getCpuLoad() + "%\n";
        }
        text += "MIDI bytes = " + MidiSynthDeviceService.getMidiByteCount();
        mLatencyLog.setText(text);
        // Can't change flag when running.
        mLatencyCheckBox.setEnabled(!mLatencyController.isRunning());
    }

    @Override
     public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (hasFocus) {
            // Start updating the latency view.
            mLatencyHandler.post(mLatencyRunnable);
            // Start generating fake key events.
            FakeKeyGenerator.getInstance().start();
        } else {
            // Stop the background tasks.
            mLatencyHandler.removeCallbacks(mLatencyRunnable);
            FakeKeyGenerator.getInstance().stop();
        }
    }

    private void setupMidi() {
        // Setup MIDI
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);

        MidiDeviceInfo synthInfo =  MidiTools.findDevice(mMidiManager, "Mobileer",
                "SynthExample");
        int portIndex = 0;
        mPortSelector = new MidiOutputPortConnectionSelector(mMidiManager, this,
                R.id.spinner_synth_sender, synthInfo, portIndex);
        mPortSelector.setConnectedListener(new MyPortsConnectedListener());
    }

    private void closeSynthResources() {
        if (mPortSelector != null) {
            Log.i(TAG,"closeSynthResources() closing port ==========================");
            mPortSelector.close();
            mPortSelector.onDestroy();
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

    public void onToggleLowLatency(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        mLatencyController.setLowLatencyEnabled(checked);
    }

    public void onToggleAutoSize(View view) {
        boolean checked = ((CheckBox) view).isChecked();
        mLatencyController.setAutoSizeEnabled(checked);
    }

    @Override
    public void onDestroy() {
        Log.i(TAG,"onDestroy() called ==========================");
        closeSynthResources();
        super.onDestroy();
    }

}
