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

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.midi20.obsolete.PolyTouchDecoder;
import com.mobileer.miditools.midi20.protocol.MidiPacketBase;
import com.mobileer.miditools.midi20.protocol.PacketDecoder;
import com.mobileer.miditools.midi20.protocol.SysExDecoder;
import com.mobileer.miditools.midi20.tools.Midi;

import java.io.IOException;

/**
 * Convert incoming MIDI messages to a string and write them to a ScopeLogger.
 * Assume that messages have been aligned using a MidiFramer.
 */
public class LoggingReceiver extends MidiReceiver {
    public static final String TAG = "MidiScope";
    private static final long NANOS_PER_MILLISECOND = 1000000L;
    private static final long NANOS_PER_SECOND = NANOS_PER_MILLISECOND * 1000L;
    private long mStartTime;
    private ScopeLogger mLogger;
    private long mLastTimeStamp = 0;
    MidiPacketBase packet = MidiPacketBase.create();
    PolyTouchDecoder polyTouchDecoder = new PolyTouchDecoder();
    SysExDecoder sysExDecoder = new SysExDecoder();

    public LoggingReceiver(ScopeLogger logger) {
        mStartTime = System.nanoTime();
        mLogger = logger;
    }

    /*
     * @see android.media.midi.MidiReceiver#onSend(byte[], int, int, long)
     */
    @Override
    public void onSend(byte[] data, int offset, int count, long timestamp)
            throws IOException {
        StringBuilder sb = new StringBuilder();
        if (timestamp == 0) {
            sb.append(String.format("-----0----: "));
        } else {
            long monoTime = timestamp - mStartTime;
            long delayTimeNanos = timestamp - System.nanoTime();
            int delayTimeMillis = (int)(delayTimeNanos / NANOS_PER_MILLISECOND);
            double seconds = (double) monoTime / NANOS_PER_SECOND;
            // Mark timestamps that are out of order.
            sb.append((timestamp < mLastTimeStamp) ? "*" : " ");
            mLastTimeStamp = timestamp;
            sb.append(String.format("%10.3f (%2d): ", seconds, delayTimeMillis));
        }
        sb.append(MidiPrinter.formatBytes(data, offset, count));
        sb.append(": ");
        sb.append(MidiPrinter.formatMessage(data, offset, count));

        Log.i(TAG, "onSend() offset = " + offset + ", count = " + count);
        if ((data[offset] & 0x0FF) == Midi.SYSEX_START) {
            boolean done = sysExDecoder.decode(data, offset, count, packet);
            if (done) {
                Log.i(TAG, "packet = " + packet);
                sb.append(MidiPacketPrinter.formatPacket(packet));
            }
        } else if ((data[offset] & 0x0F0) == 0x0A0) {
            boolean done = polyTouchDecoder.decode(data, offset, count, packet);
            if (done) {
                Log.i(TAG, "packet = " + packet);
                sb.append(MidiPacketPrinter.formatPacket(packet));
            }
        }

        String text = sb.toString();
        mLogger.log(text);
        Log.i(TAG, text);
    }

}
