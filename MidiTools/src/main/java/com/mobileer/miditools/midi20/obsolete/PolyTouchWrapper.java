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

import com.mobileer.miditools.midi20.obsolete.NextGenWrapper;
import com.mobileer.miditools.midi20.protocol.MidiPacketBase;
import com.mobileer.miditools.midi20.protocol.PacketDecoder;
import com.mobileer.miditools.midi20.protocol.PacketEncoder;

public class PolyTouchWrapper {

    public final static int TOUCH_OPCODE = 0x0A0;
    public final static int TOUCH_TYPE_START = 0x04;
    public final static int TOUCH_TYPE_MIDDLE = 0x08;
    public final static int TOUCH_TYPE_END = 0x0C;
    public final static int TOUCH_OPCODE_MASK = 0x0F0;
    public final static int TOUCH_TYPE_MASK = 0x00C;

}
