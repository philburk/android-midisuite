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

public class SysExEncoder implements PacketEncoder {

    private byte[] mData = new byte[5 * 4 + 3];
    private int mCursor = 0;

    @Override
    public int encode(MidiPacketBase packet) {
        write(0xF0);
        write(0x7D);
        int numWords = packet.wordCount();
        int sizeEncoding = numWords - 1;
        for (int i = 0; i < numWords; i++) {
            long word = packet.getWord(i);
            int combo = sizeEncoding << 4;
            combo |= ((word >> (31 - 3)) & 0x08);
            combo |= ((word >> (23 - 2)) & 0x04);
            combo |= ((word >> (15 - 1)) & 0x02);
            combo |= ((word >> (7 - 0)) & 0x01);
            write(combo);
            write((int)((word >> 24) & 0x7F));
            write((int)((word >> 16) & 0x7F));
            write((int)((word >> 8) & 0x7F));
            write((int)(word & 0x7F));
        }
        write(0xF7);
        return mCursor;
    }

    public void write(int b) {
        mData[mCursor++] = (byte)b;
    }

    public byte[] getBytes() {
        return mData;
    }
}
