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
 * Decode packets from raw bytes in Big Endian order.
 */

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class RawByteDecoder implements PacketDecoder {

    private IntBuffer mIntBuffer;

    public RawByteDecoder() {
    }

    public RawByteDecoder(byte[] data, int offset, int length) {
        wrap(data, offset, length);
    }

    public void wrap(byte[] data, int offset, int length) {
        ByteBuffer buffer = ByteBuffer.wrap(data, offset, length);
        mIntBuffer = buffer.asIntBuffer();
    }

    /**
     * Fill in packet.
     * @param packet
     * @return true if we got a full packet
     */
    @Override
    public boolean decode(UniversalMidiPacket packet) {
        if (mIntBuffer.remaining() == 0) return false;
        int i = 0;
        int firstWord = mIntBuffer.get();
        packet.setWord(i++, firstWord); // Contains MessageType so we know the size.
        int wordsLeft = packet.wordCount() - 1;
        if (wordsLeft > mIntBuffer.remaining()) return false;
        while (wordsLeft > 0) {
            int word = mIntBuffer.get();
            packet.setWord(i++, word);
            wordsLeft--;
        }
        return true;
    }


}
