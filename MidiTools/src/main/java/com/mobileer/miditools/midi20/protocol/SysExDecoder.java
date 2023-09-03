/*
 * Copyright 2018 The Android Open Source Project
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

package com.mobileer.miditools.midi20.protocol;
/**
 * Decode a MIDI 2.0 packet from a SysEx.
 * This was used when prototyping MIDI 2.0.
 * TODO: Delete, no longer needed.
 */
public class SysExDecoder implements PacketDecoder {

    private byte[] mData;
    private int mValid;
    private int mCursor;
    private int mLimit;

    public SysExDecoder() {
    }
    public SysExDecoder(byte[] data, int offset, int length) {
        wrap(data, offset, length);
    }

    public void wrap(byte[] data, int offset, int length) {
        System.out.println("wrap: offset = " + offset + ", length = " + length);
        mData = data;
        mCursor = offset;
        mLimit = offset + length;
    }

    @Override
    public boolean decode(UniversalMidiPacket packet) {
        System.out.println("decoder: mCursor = " + mCursor + ", mLimit = " + mLimit);
        if (mCursor >= mLimit) return false;

        if (read() != 0xF0) return false;
        if (read() != (byte)0x7D) return false;
        int combo = read();
        int numWords = (combo >> 4) + 1;
        if (numWords > 4) return false;
        for (int i = 0; i < numWords; i++) {
            if (combo >= 0x80) return false;

            int a = read();
            if (a >= 0x80) return false;
            a |= (combo << 4) & 0x080;

            int b = read();
            if (b >= 0x80) return false;
            b |= (combo << 5) & 0x080;

            int c = read();
            if (c >= 0x80) return false;
            c |= (combo << 6) & 0x080;

            int d = read();
            if (d >= 0x80) return false;
            d |= (combo << 7) & 0x080;

            long word = (a << 24) | (b << 16) | (c << 8) | d;
            packet.setWord(i, word);

            combo = read();
        }
        System.out.println("combo = " + combo);
        if (combo != 0x0F7) return false;
        return (combo == 0x0F7);
    }

    /**
     * Read next byte from the stream as an unsigned byte.
     * @return
     */
    public int read() {
        int data = mData[mCursor++] & 0x0FF;
        System.out.println("data = " + data);
        return data;
    }

}
