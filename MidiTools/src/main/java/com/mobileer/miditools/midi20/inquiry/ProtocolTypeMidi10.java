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

public class ProtocolTypeMidi10 extends ProtocolType {

    public int getType() {
        return TYPE_MIDI_1_0;
    }

    @Override
    public void encode(MidiWriter buffer) {
        buffer.write(getType());
        buffer.write2(0); // reserved
        buffer.write2(0);
    }

    @Override
    public void decode(MidiReader reader) {
        int type = reader.read();
        assert (type == TYPE_MIDI_1_0);
        reader.read2(); // reserved TODO verify zero?
        reader.read2(); // reserved TODO verify zero?
    }
}
