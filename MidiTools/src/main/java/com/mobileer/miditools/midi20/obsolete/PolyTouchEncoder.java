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

package com.mobileer.miditools.midi20.obsolete;

import com.mobileer.miditools.midi20.protocol.MidiPacketBase;
import com.mobileer.miditools.midi20.protocol.PacketEncoder;

public class PolyTouchEncoder extends PolyTouchWrapper implements PacketEncoder {

    private byte[] mData = new byte[4 * 4 * 2]; // 4 words * 4 bytes * 2 bigger
    private int mCursor = 0;

    @Override
    public int encode(MidiPacketBase packet) {
        mCursor = 0;
        int wordCount = packet.wordCount();
        for (int i = 0; i < wordCount; i++) {
            long word = packet.getWord(i);
            int status = TOUCH_OPCODE;

            if (i == 0) status |= TOUCH_TYPE_START;
            else status |= TOUCH_TYPE_MIDDLE;
            encodeMessage(status, (int) (word >> 16));

            status = TOUCH_OPCODE;
            if (i == wordCount - 1) status |= TOUCH_TYPE_END;
            else status |= TOUCH_TYPE_MIDDLE;
            encodeMessage(status, (int) word);
        }

        return mCursor;
    }

    public void write(int b) {
        mData[mCursor++] = (byte) b;
    }

    public byte[] getBytes() {
        return mData;
    }

    public void encodeMessage(int status, int word) {
        int command = status | ((word >> 14) & 2) // high bit of MSB
                | ((word >> 7) & 1); // high bit of LSB

        //System.out.printf("encode: word = 0x%04X, status = 0x%02X, command = 0x%02X\n",
        //        word, status, command);
        write(command);
        write(0x07F & (word >> 8)); // 7 bits of MSB
        write(0x07F & word); // 7 bits of LSB
    }

}
