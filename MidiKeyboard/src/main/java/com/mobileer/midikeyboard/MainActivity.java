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
import android.media.midi.MidiDevice;
import android.media.midi.MidiManager;
import android.media.midi.MidiOutputPort;
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
import com.mobileer.miditools.midi20.inquiry.NegotiatingThread;
import com.mobileer.miditools.midi20.protocol.MidiPacketBase;
import com.mobileer.miditools.midi20.protocol.PacketEncoder;
import com.mobileer.miditools.midi20.protocol.RawByteEncoder;
import com.mobileer.miditools.midi20.protocol.SysExEncoder;
import com.mobileer.miditools.midi20.tools.Midi;

import java.io.IOException;

/**
 * Main activity for the keyboard app.
 */
public class MainActivity extends Activity {
    private static final String TAG = "MidiKeyboard";
    private static final int DEFAULT_VELOCITY = 64;

    private MidiInputPortSelector mKeyboardReceiverSelector;
    private MidiOutputPort mOutputPort;
    private MusicKeyboardView mKeyboard;
    private Button mProgramButton;
    private MidiManager mMidiManager;
    private int mChannel; // ranges from 0 to 15
    private int[] mPrograms = new int[MidiConstants.MAX_CHANNELS]; // ranges from 0 to 127
    private byte[] mByteBuffer = new byte[3];
    private NegotiatingThread mNegotiator;

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
                this, R.id.spinner_receivers) {
            @Override
            public void onInputOpened(MidiDevice device, int portIndex) {
                // Does the device also have an output port with the same index?
                if (!NegotiatingThread.isEnabled()) {
                    Log.i(TAG, "CI Negotiation disabled");
                } else {
                    if (device.getInfo().getOutputPortCount() <= portIndex) {
                        Log.i(TAG, "output port counts too low for CI negotiation, "
                                + device.getInfo().getOutputPortCount() + " <= " + portIndex);
                    } else {
                        // Open a paired Output port for receiving the negotiation messages.
                        mOutputPort = device.openOutputPort(portIndex);
                        if (mOutputPort == null) {
                            Log.i(TAG, "Cannot open output port for CI negotiation.");
                        } else {
                            // Bidirectional device so try to negotiate.
                            mNegotiator = new NegotiatingThread();
                            mOutputPort.connect(mNegotiator);
                            mNegotiator.setTargetReceiver(getReceiver());
                            mNegotiator.setInitiator(true);
                            mNegotiator.start();
                        }
                    }
                }
            }

            @Override
            public void onClose() {
                if (mNegotiator != null) {
                    mNegotiator.stop();
                    mNegotiator = null;
                }
            }
        };

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

        if (use10()) {
            midiCommand(MidiConstants.STATUS_PROGRAM_CHANGE + mChannel, program);
        } else {
            programChange2(mChannel, program);
        }
        mPrograms[mChannel] = program;
        updateProgramText();
    }

    private void updateProgramText() {
        mProgramButton.setText("" + mPrograms[mChannel]);
    }

    private void noteOff(int channel, int pitch, int velocity) {
        if (use10()) {
            midiCommand(MidiConstants.STATUS_NOTE_OFF +channel, pitch, velocity);
        } else {
            noteOff2(channel, pitch, velocity);
        }
    }

    private boolean use10() {
        return (mNegotiator != null) && (mNegotiator.getNegotiatedVersion() == Midi.VERSION_1_0);
    }

    private void noteOn(int channel, int pitch, int velocity) {
        if (use10()) {
            midiCommand(MidiConstants.STATUS_NOTE_ON +channel, pitch, velocity);
        } else {
            noteOn2(channel, pitch, velocity);
        }
    }

    private void programChange2(int channel, int program) {
        MidiPacketBase packet = MidiPacketBase.create();
        packet.programChange(program, 1234);
        packet.setChannel(channel);
        sendPacket(packet);
    }

    private void noteOn2(int channel, int pitch, int velocity) {
        MidiPacketBase packet = MidiPacketBase.create();
        packet.noteOn(pitch, velocity);
        packet.setChannel(channel);
        sendPacket(packet);
    }

    private void noteOff2(int channel, int pitch, int velocity) {
        MidiPacketBase packet = MidiPacketBase.create();
        packet.noteOff(pitch, velocity);
        packet.setChannel(channel);
        sendPacket(packet);
    }

    private void sendPacket(MidiPacketBase packet) {
        //PacketEncoder encoder = new SysExEncoder();
        RawByteEncoder encoder = new RawByteEncoder();
        int len = encoder.encode(packet);
        long now = System.nanoTime();
        byte[] data = encoder.getBytes();
        Log.i(TAG, "packet = " + packet);
        Log.i(TAG, "noteOn2() len = " + len + ", b[0] = " + (((int)data[0]) & 0xFF));
        midiSend(data, len, now);
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
        if (mKeyboardReceiverSelector != null) {
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
}
