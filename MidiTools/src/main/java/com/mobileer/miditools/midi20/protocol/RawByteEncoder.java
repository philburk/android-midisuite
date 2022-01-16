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

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Encode packets as raw bytes in Big Endian order.
 */
public class RawByteEncoder implements PacketEncoder {

    private final ByteArrayOutputStream mData = new ByteArrayOutputStream();
    private byte pad[] = new byte[4];

    @Override
    public int encode(UniversalMidiPacket packet) {
        int numWords = packet.wordCount();
        for (int i = 0; i < numWords; i++) {
            long word = packet.getWord(i);
            // Convert to bytes in network-order.
            pad[0] = (byte)(word >> 24);
            pad[1] = (byte)(word >> 16);
            pad[2] = (byte)(word >> 8);
            pad[3] = (byte)(word);
            try {
                mData.write(pad);
            } catch (IOException e) {
                return -1; // This will almost certainly never happen.
            }
        }
        return numWords * 4;
    }

    public byte[] getBytes() {
        return mData.toByteArray();
    }
}
