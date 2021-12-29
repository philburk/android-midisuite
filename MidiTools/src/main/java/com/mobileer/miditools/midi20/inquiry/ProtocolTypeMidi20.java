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

package com.mobileer.miditools.midi20.inquiry;

import com.mobileer.miditools.midi20.tools.MidiReader;
import com.mobileer.miditools.midi20.tools.MidiWriter;

public class ProtocolTypeMidi20 extends ProtocolType {
    private int mVersion;
    private int mSubType;

    public int getType() {
        return TYPE_MIDI_NEW;
    }

    public int getVersion() {
        return mVersion;
    }

    public void setVersion(int mVersion) {
        this.mVersion = mVersion;
    }

    public int getSubType() {
        return mSubType;
    }

    public void setSubType(int mSubType) {
        this.mSubType = mSubType;
    }

    @Override
    public void encode(MidiWriter buffer) {
        buffer.write(getType());
        buffer.write(mVersion);
        buffer.write(mSubType);
        buffer.write2(0);
    }

    @Override
    public void decode(MidiReader reader) {
        int type = reader.read();
        assert (type == TYPE_MIDI_NEW);
        mVersion = reader.read();
        mSubType = reader.read();
        reader.read2(); // reserved TODO verify zero?
    }
}
