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

public class SysExDecoder implements PacketDecoder {

    private byte[] mData;
    private int mValid = 0;
    private int mCursor = 0;

    /**
     * Fill in packet.
     * @param packet
     * @return
     */
    @Override
    public boolean decode(byte[] stream, int offset, int len, MidiPacketBase packet) {
        // TODO handle partial sysexes
        mData = stream;
        mCursor = offset;

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
        return (combo == 0x0F7);
    }

    /**
     * Read next byte from the stream as an unsigned byte.
     * @return
     */
    public int read() {
        return mData[mCursor++] & 0x0FF;
    }

}
