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

package com.mobileer.miditools.midi20.tools;

public class MidiWriter {
    private byte[] mData = new byte[128];
    private int cursor;

    public byte[] getData() {
        return mData;
    }
    public int getCursor() {
        return cursor;
    }

    /**
     * Write a single byte to the buffer.
     * @param value
     */
    public void write(int value) {
        mData[cursor++] = (byte) value;
    }

    /**
     * Write bottom two bytes, LSB first.
     * @param value
     */
    public void write2(int value) {
        write(value); // LSB first
        write(value>>8);
    }

    public void write3(int value) {
        write2(value);
        write(value>>16);
    }
    public void write4(int value) {
        write3(value);
        write(value>>24);
    }

    public void write28bits(int value) {
        // Little Endian, 7-bits at a time
        write(value & 0x7F);
        write((value >> 7) & 0x7F);
        write((value >> 14) & 0x7F);
        write((value >> 21) & 0x7F);
    }

}
