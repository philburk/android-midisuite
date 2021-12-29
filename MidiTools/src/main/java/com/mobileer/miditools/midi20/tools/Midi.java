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

public class Midi {
    public static final int SYSEX_START = 0xF0;
    public static final int SYSEX_END = 0xF7;
    public static final int SYSEX_UNIVERSAL = 0x7E;
    public static final int SYSEX_SUBID1_CI = 0x0D;

    public static final int CI_TOFROM_PORT = 0x7F;
    public static final int CI_VERSION = 0x00;

    public static final int CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION = 0x10;
    public static final int CI_SUBID2_REPLY_PROTOCOL_NEGOTIATION = 0x11;
    public static final int CI_SUBID2_SET_PROTOCOL = 0x12;
    public static final int CI_SUBID2_TEST_INITIATOR_TO_RESPONDER = 0x13;
    public static final int CI_SUBID2_TEST_RESPONDER_TO_INITIATOR = 0x14;
    public static final int CI_SUBID2_CONFIRM_NEW_PROTOCOL = 0x15;

    public static final int VERSION_1_0 = 10;
    public static final int VERSION_2_0 = 20;
    public static final int VERSION_3_0 = 30; // Just for testing protocol negotiation
}
