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

    public void write(int value) {
        mData[cursor++] = (byte) value;
    }

    public void write2(int value) {
        write(value>>8);
        write(value);
    }

    public void write3(int value) {
        write(value>>16);
        write2(value);
    }

    public void write28bits(int value) {
        // Big Endian, 7-bit data
        write((value >> 21) & 0x7F);
        write((value >> 14) & 0x7F);
        write((value >> 7) & 0x7F);
        write(value & 0x7F);
    }

    public void writeManufacturerId(int id) {
        if (id < 0x7F) {
            write(id);
        } else {
            write3(id);
        }
    }
}
