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

package com.android.miditools;

import static org.junit.Assert.*;

import android.media.midi.MidiReceiver;

import org.junit.Test;

import java.io.IOException;
import java.util.ArrayList;

// Uncomment this import if you want to test the internal MidiFramer.
// import com.android.internal.midi.MidiFramer;

/**
 * Unit Tests for the MidiFramer
 */
public class TestMidiFramer {

    // Store a complete MIDI message.
    static class MidiMessage {
        public final byte[] data;
        public final long timestamp;
        public final long timeReceived;

        MidiMessage(byte[] buffer, long timestamp) {
            this(buffer, 0, buffer.length, timestamp);
        }

        MidiMessage(byte[] buffer, int offset, int length, long timestamp) {
            timeReceived = System.nanoTime();
            data = new byte[length];
            System.arraycopy(buffer, offset, data, 0, length);
            this.timestamp = timestamp;
        }

        // Check whether these two messages are the same.
        public void check(MidiMessage other) {
            assertEquals("data.length", data.length, other.data.length);
            assertEquals("data.timestamp", timestamp, other.timestamp);
            for (int i = 0; i < data.length; i++) {
                assertEquals("data[" + i + "]", data[i], other.data[i]);
            }
        }
    }

    // Store received messages in an array.
    class MyLoggingReceiver extends MidiReceiver {
        ArrayList<MidiMessage> messages = new ArrayList<MidiMessage>();

        @Override
        public void onSend(byte[] data, int offset, int count,
                long timestamp) {
            messages.add(new MidiMessage(data, offset, count, timestamp));
        }

        public int getMessageCount() {
            return messages.size();
        }

        public MidiMessage getMessage(int index) {
            return messages.get(index);
        }
    }

    /**
     * Send the original messages and verify that we receive back the expected
     * messages.
     *
     * @param original
     * @param expected
     * @throws IOException
     */
    private void checkSequence(MidiMessage[] original, MidiMessage[] expected)
            throws IOException {
        MyLoggingReceiver receiver = new MyLoggingReceiver();
        MidiFramer framer = new MidiFramer(receiver);
        for (MidiMessage message : original) {
            framer.send(message.data, 0, message.data.length,
                    message.timestamp);
        }
        assertEquals("command count", expected.length,
                receiver.getMessageCount());
        for (int i = 0; i < expected.length; i++) {
            expected[i].check(receiver.getMessage(i));
        }
    }

    private void checkSequence(byte[][] original, byte[][] expected,
            long timestamp) throws IOException {
        int index = 0;
        MidiMessage[] originalMessages = new MidiMessage[original.length];
        for (byte[] data : original) {
            originalMessages[index++] = new MidiMessage(data, timestamp);
        }
        index = 0;
        MidiMessage[] expectedMessages = new MidiMessage[expected.length];
        for (byte[] data : expected) {
            expectedMessages[index++] = new MidiMessage(data, timestamp);
        }
        checkSequence(originalMessages, expectedMessages);
    }

    // Send a NoteOn through the MidiFramer
    @Test
    public void testSimple() throws IOException {
        byte[] data = { (byte) 0x90, 0x45, 0x32 };
        MyLoggingReceiver receiver = new MyLoggingReceiver();
        MidiFramer framer = new MidiFramer(receiver);
        assertEquals("command byte", (byte) 0x90, data[0]);
        framer.send(data, 0, data.length, 0L);
        assertEquals("command count", 1, receiver.getMessageCount());
        MidiMessage expected = new MidiMessage(data, 0, data.length, 0L);
        MidiMessage message = receiver.getMessage(0);
        expected.check(message);
    }

    // Test message based testing tool using a NoteOn
    @Test
    public void testSimpleSequence() throws IOException {
        long timestamp = 8263518L;
        byte[] data = { (byte) 0x90, 0x45, 0x32 };
        MidiMessage[] original = { new MidiMessage(data, timestamp) };
        checkSequence(original, original);
    }

    // NoteOn then NoteOff using running status
    @Test
    public void testRunningArrays() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { (byte) 0x90, 0x45, 0x32, 0x45, 0x00 } };
        byte[][] expected = { { (byte) 0x90, 0x45, 0x32 },
                { (byte) 0x90, 0x45, 0x00 } };
        checkSequence(original, expected, timestamp);
    }

    // Start with unresolved running status that should be ignored
    @Test
    public void testStartMiddle() throws IOException {
        long timestamp = 837518L;
        byte[][] original = {
                { 0x23, 0x34, (byte) 0x90, 0x45, 0x32, 0x45, 0x00 } };
        byte[][] expected = { { (byte) 0x90, 0x45, 0x32 },
                { (byte) 0x90, 0x45, 0x00 } };
        checkSequence(original, expected, timestamp);
    }

    @Test
    public void testTwoOn() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { (byte) 0x90, 0x45, 0x32, 0x47, 0x63 } };
        byte[][] expected = { { (byte) 0x90, 0x45, 0x32 },
                { (byte) 0x90, 0x47, 0x63 } };
        checkSequence(original, expected, timestamp);
    }

    @Test
    public void testThreeOn() throws IOException {
        long timestamp = 837518L;
        byte[][] original = {
                { (byte) 0x90, 0x45, 0x32, 0x47, 0x63, 0x49, 0x23 } };
        byte[][] expected = { { (byte) 0x90, 0x45, 0x32 },
                { (byte) 0x90, 0x47, 0x63 }, { (byte) 0x90, 0x49, 0x23 } };
        checkSequence(original, expected, timestamp);
    }

    // A RealTime message before a NoteOn
    @Test
    public void testRealTimeBefore() throws IOException {
        long timestamp = 8375918L;
        byte[][] original = { { MidiConstants.STATUS_TIMING_CLOCK, (byte) 0x90,
                0x45, 0x32 } };
        byte[][] expected = { { MidiConstants.STATUS_TIMING_CLOCK },
                { (byte) 0x90, 0x45, 0x32 } };
        checkSequence(original, expected, timestamp);
    }

    // A RealTime message in the middle of a NoteOn
    @Test
    public void testRealTimeMiddle1() throws IOException {
        long timestamp = 8375918L;
        byte[][] original = { { (byte) 0x90, MidiConstants.STATUS_TIMING_CLOCK,
                0x45, 0x32 } };
        byte[][] expected = { { MidiConstants.STATUS_TIMING_CLOCK },
                { (byte) 0x90, 0x45, 0x32 } };
        checkSequence(original, expected, timestamp);
    }

    @Test
    public void testRealTimeMiddle2() throws IOException {
        long timestamp = 8375918L;
        byte[][] original = { { (byte) 0x90, 0x45,
                MidiConstants.STATUS_TIMING_CLOCK, 0x32 } };
        byte[][] expected = { { (byte) 0xF8 }, { (byte) 0x90, 0x45, 0x32 } };
        checkSequence(original, expected, timestamp);
    }

    // A RealTime message after a NoteOn
    @Test
    public void testRealTimeAfter() throws IOException {
        long timestamp = 8375918L;
        byte[][] original = { { (byte) 0x90, 0x45, 0x32,
                MidiConstants.STATUS_TIMING_CLOCK } };
        byte[][] expected = { { (byte) 0x90, 0x45, 0x32 }, { (byte) 0xF8 } };
        checkSequence(original, expected, timestamp);
    }

    // Break up running status across multiple messages
    @Test
    public void testPieces() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { (byte) 0x90, 0x45 }, { 0x32, 0x47 },
                { 0x63, 0x49, 0x23 } };
        byte[][] expected = { { (byte) 0x90, 0x45, 0x32 },
                { (byte) 0x90, 0x47, 0x63 }, { (byte) 0x90, 0x49, 0x23 } };
        checkSequence(original, expected, timestamp);
    }

    // Break up running status into single byte messages
    @Test
    public void testByByte() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { (byte) 0x90 }, { 0x45 }, { 0x32 }, { 0x47 },
                { 0x63 }, { 0x49 }, { 0x23 } };
        byte[][] expected = { { (byte) 0x90, 0x45, 0x32 },
                { (byte) 0x90, 0x47, 0x63 }, { (byte) 0x90, 0x49, 0x23 } };
        checkSequence(original, expected, timestamp);
    }

    @Test
    public void testControlChange() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { MidiConstants.STATUS_CONTROL_CHANGE, 0x07, 0x52,
                0x0A, 0x63 } };
        byte[][] expected = { { (byte) 0xB0, 0x07, 0x52 },
                { (byte) 0xB0, 0x0A, 0x63 } };
        checkSequence(original, expected, timestamp);
    }

    @Test
    public void testProgramChange() throws IOException {
        long timestamp = 837518L;
        byte[][] original = {
                { MidiConstants.STATUS_PROGRAM_CHANGE, 0x05, 0x07 } };
        byte[][] expected = { { (byte) 0xC0, 0x05 }, { (byte) 0xC0, 0x07 } };
        checkSequence(original, expected, timestamp);
    }

    // ProgramChanges, SysEx, ControlChanges
    @Test
    public void testAck() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { MidiConstants.STATUS_PROGRAM_CHANGE, 0x05, 0x07,
                MidiConstants.STATUS_SYSTEM_EXCLUSIVE, 0x7E, 0x03, 0x7F, 0x21,
                (byte) 0xF7, MidiConstants.STATUS_CONTROL_CHANGE, 0x07, 0x52,
                0x0A, 0x63 } };
        byte[][] expected = { { (byte) 0xC0, 0x05 }, { (byte) 0xC0, 0x07 },
                { (byte) 0xF0, 0x7E, 0x03, 0x7F, 0x21, (byte) 0xF7 },
                { (byte) 0xB0, 0x07, 0x52 }, { (byte) 0xB0, 0x0A, 0x63 } };
        checkSequence(original, expected, timestamp);
    }

    // Split a SysEx across 3 messages.
    @Test
    public void testSplitSysEx() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { MidiConstants.STATUS_SYSTEM_EXCLUSIVE, 0x7E },
                { 0x03, 0x7F }, { 0x21, (byte) 0xF7 } };
        byte[][] expected = { { (byte) 0xF0, 0x7E }, { 0x03, 0x7F },
                { 0x21, (byte) 0xF7 } };
        checkSequence(original, expected, timestamp);
    }

    // RealTime in the middle of a SysEx
    @Test
    public void testRealSysEx() throws IOException {
        long timestamp = 837518L;
        byte[][] original = { { MidiConstants.STATUS_SYSTEM_EXCLUSIVE, 0x7E,
                0x03, MidiConstants.STATUS_TIMING_CLOCK, 0x7F, 0x21,
                (byte) 0xF7 } };
        byte[][] expected = { { (byte) 0xF0, 0x7E, 0x03 }, { (byte) 0xF8 },
                { 0x7F, 0x21, (byte) 0xF7 } };
        checkSequence(original, expected, timestamp);
    }

}
