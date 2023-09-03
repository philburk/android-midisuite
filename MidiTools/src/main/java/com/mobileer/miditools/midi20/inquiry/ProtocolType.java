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

/**
 * Identify a MIDI protocol.
 * This is used for Capabilities Inquiry.
 */
public abstract class ProtocolType {
    public final static int TYPE_MIDI_1_0 = 0;
    public final static int TYPE_MIDI_2 = 1;
    public final static int TYPE_MANUFACTURER_SPECIFIC = 2;

    public abstract int getType();

    public abstract void encode(MidiWriter buffer);

    public static ProtocolType create(int type) {
        if (type == TYPE_MIDI_1_0) {
            return new ProtocolTypeMidi10();
        } else if (type == TYPE_MIDI_2) {
            return new ProtocolTypeMidi20();
        }
        return null;
    }

    public abstract void decode(MidiReader reader);
}
