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

public class MidiReader {
    private byte[] mData;
    private int mCursor;

    private MidiReader() {}

    public MidiReader(byte[] data, int offset) {
        mData = data;
        mCursor = offset;
    }

    public int read() {
        return ((int)mData[mCursor++]) & 0xFF;
    }

    public int peek() {
        return ((int)mData[mCursor]) & 0xFF;
    }

    public int read28bits() {
        return (read() << 21) | (read() << 14) | (read() << 7) | read();
    }

    public int readManufacturerId() {
        int id = read();
        if (id != 0) {
            return id;
        }
        return read2();
    }

    public int read2() {
        return read() | (read() << 8);
    }

    public int read3() {
        return read2() | (read() << 16);
    }

    public int read4() {
        return read3() | (read() << 24);
    }

    public int getCursor() {
        return mCursor;
    }
}
