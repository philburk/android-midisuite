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

package com.mobileer.midikeyboard;

import android.app.Activity;
import android.content.pm.PackageManager;
import android.media.midi.MidiManager;
import android.media.midi.MidiReceiver;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;

import com.mobileer.miditools.MidiConstants;
import com.mobileer.miditools.MidiInputPortSelector;
import com.mobileer.miditools.MusicKeyboardView;

import java.io.IOException;

/**
 * Main activity for the keyboard app.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MidiKeyboard";
    private static final int DEFAULT_VELOCITY = 64;

    private MidiInputPortSelector mKeyboardReceiverSelector;
    private MusicKeyboardView mKeyboard;
    private Button mProgramButton;
    private MidiManager mMidiManager;
    private int mChannel; // ranges from 0 to 15
    private int[] mPrograms = new int[MidiConstants.MAX_CHANNELS]; // ranges from 0 to 127
    private byte[] mByteBuffer = new byte[3];

    public class ChannelSpinnerActivity implements AdapterView.OnItemSelectedListener {
        @Override
        public void onItemSelected(AdapterView<?> parent, View view,
                                   int pos, long id) {
            mChannel = pos & 0x0F;
            updateProgramText();
        }

        @Override
        public void onNothingSelected(AdapterView<?> parent) {
        }
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_MIDI)) {
            setupMidi();
        } else {
            Toast.makeText(this, "MIDI not supported!", Toast.LENGTH_LONG)
                    .show();
        }

        mProgramButton = (Button) findViewById(R.id.button_program);

        Spinner spinner = (Spinner) findViewById(R.id.spinner_channels);
        spinner.setOnItemSelectedListener(new ChannelSpinnerActivity());
    }

    private void setupMidi() {
        mMidiManager = (MidiManager) getSystemService(MIDI_SERVICE);
        if (mMidiManager == null) {
            Toast.makeText(this, "MidiManager is null!", Toast.LENGTH_LONG)
                    .show();
            return;
        }

        // Setup Spinner that selects a MIDI input port.
        mKeyboardReceiverSelector = new MidiInputPortSelector(mMidiManager,
                this, R.id.spinner_receivers);

        mKeyboard = (MusicKeyboardView) findViewById(R.id.musicKeyboardView);
        mKeyboard.addMusicKeyListener(new MusicKeyboardView.MusicKeyListener() {
            @Override
            public void onKeyDown(int keyIndex) {
                noteOn(mChannel, keyIndex, DEFAULT_VELOCITY);
            }

            @Override
            public void onKeyUp(int keyIndex) {
                noteOff(mChannel, keyIndex, DEFAULT_VELOCITY);
            }
        });
    }

    public void onProgramSend(View view) {
        midiCommand(MidiConstants.STATUS_PROGRAM_CHANGE + mChannel, mPrograms[mChannel]);
    }

    public void onProgramDelta(View view) {
        Button button = (Button) view;
        int delta = Integer.parseInt(button.getText().toString());
        changeProgram(delta);
    }

    private void changeProgram(int delta) {
        int program = mPrograms[mChannel];
        program += delta;
        if (program < 0) {
            program = 0;
        } else if (program > 127) {
            program = 127;
        }
        midiCommand(MidiConstants.STATUS_PROGRAM_CHANGE + mChannel, program);
        mPrograms[mChannel] = program;
        updateProgramText();
    }

    private void updateProgramText() {
        mProgramButton.setText("" + mPrograms[mChannel]);
    }

    private void noteOff(int channel, int pitch, int velocity) {
        midiCommand(MidiConstants.STATUS_NOTE_OFF + channel, pitch, velocity);
    }

    private void noteOn(int channel, int pitch, int velocity) {
        midiCommand(MidiConstants.STATUS_NOTE_ON + channel, pitch, velocity);
    }

    private void midiCommand(int status, int data1, int data2) {
        mByteBuffer[0] = (byte) status;
        mByteBuffer[1] = (byte) data1;
        mByteBuffer[2] = (byte) data2;
        long now = System.nanoTime();
        midiSend(mByteBuffer, 3, now);
    }

    private void midiCommand(int status, int data1) {
        mByteBuffer[0] = (byte) status;
        mByteBuffer[1] = (byte) data1;
        long now = System.nanoTime();
        midiSend(mByteBuffer, 2, now);
    }

    private void closeSynthResources() {
        if (mKeyboardReceiverSelector != null) {
            mKeyboardReceiverSelector.close();
            mKeyboardReceiverSelector.onDestroy();
        }
    }

    @Override
    public void onDestroy() {
        closeSynthResources();
        super.onDestroy();
    }

    private void midiSend(byte[] buffer, int count, long timestamp) {
        try {
            // send event immediately
            MidiReceiver receiver = mKeyboardReceiverSelector.getReceiver();
            if (receiver != null) {
                receiver.send(buffer, 0, count, timestamp);
            }
        } catch (IOException e) {
            Log.e(TAG, "mKeyboardReceiverSelector.send() failed " + e);
        }
    }
}
