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

package com.mobileer.miditools.midi20.protocol;

public class HDMidiPacket extends MidiPacketBase {

    public static final int OPCODE_PER_NOTE_RPN = 0x0;
    public static final int OPCODE_PER_NOTE_NRPN = 0x1;
    public static final int OPCODE_RPN = 0x2;
    public static final int OPCODE_NRPN = 0x3;
    public static final int OPCODE_RELATIVE_RPN = 0x4;
    public static final int OPCODE_RELATIVE_NRPN = 0x5;
    public static final int OPCODE_PER_NOTE_PITCH_BEND = 0x6;

    public static final int OPCODE_NOTE_OFF = 0x8;
    public static final int OPCODE_NOTE_ON = 0x9;
    public static final int OPCODE_POLY_PRESSURE = 0xA;
    public static final int OPCODE_CONTROL_CHANGE = 0xB;
    public static final int OPCODE_PROGRAM_CHANGE = 0xC;
    public static final int OPCODE_CHANNEL_PRESSURE = 0xD;
    public static final int OPCODE_PITCH_BEND = 0xE;

    public static final int OPCODE_PER_NOTE_MANAGEMENT = 0xF;

    protected void setupNote(int opcode, int noteNumber, int velocity) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(opcode);
        setHeaderByte(noteNumber, 8);
        setData1MSW(velocity);
    }

    public boolean isChannelVoiceMessage() {
        return getType() == TYPE_CHANNEL_VOICE_HD;
    }

    public void noteOn(int noteNumber, int velocity) {
        setupNote(OPCODE_NOTE_ON, noteNumber, velocity);
    }

    public boolean isNoteOn() {
        return isChannelVoiceMessage() && getOpcode() == OPCODE_NOTE_ON;
    }

    public void noteOff(int noteNumber, int velocity) {
        setupNote(OPCODE_NOTE_OFF, noteNumber, velocity);
    }

    public boolean isNoteOff() {
        return isChannelVoiceMessage() && getOpcode() == OPCODE_NOTE_OFF;
    }

    @Override
    public void programChange(int bank, int program) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(OPCODE_PROGRAM_CHANGE);
        setBank(bank);
        setProgram(program);
    }

    @Override
    public void controlChange(int index, double value) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(OPCODE_CONTROL_CHANGE);
        setHeaderL77(index);
        setNormalizedControllerValue(value);
    }

    public void RPN(int index, double value) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(OPCODE_RPN);
        setHeaderL77(index);
        setNormalizedControllerValue(value);
    }

    @Override
    public void controlChange(int index, long value) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(OPCODE_CONTROL_CHANGE);
        setHeaderL77(index);
        data[1] = (int) value;
    }

    public void RPN(int index, long value) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(OPCODE_RPN);
        setHeaderL77(index);
        data[1] = (int) value;
    }

    public void NRPN(int index, long value) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(OPCODE_NRPN);
        setHeaderL77(index);
        data[1] = (int) value;
    }

    public int getControllerIndex() {
        return getHeaderL77();
    }

    @Override
    public boolean isControlChange() {
        return isChannelVoiceMessage() && getOpcode() == OPCODE_CONTROL_CHANGE;
    }

    public boolean isRPN() {
        return isChannelVoiceMessage() && getOpcode() == OPCODE_RPN;
    }

    public boolean isNRPN() {
        return isChannelVoiceMessage() && getOpcode() == OPCODE_NRPN;
    }

    /**
     * Split the index into a 7-bit bank and a 7-bit index
     * @param index
     */
    protected void setHeaderL77(int index) {
        int header = getHeader();
        header &= 0xFFFF0000; // make hole
        header |= ((index << 1) & 0x7F00); // bank
        header |= (index & 0x7F); // 7-bit index
        setHeader(header);
    }

    protected int getHeaderL77() {
        int header = getHeader();
        int bank = (header & 0x7F00) >> 1;
        int index = header & 0x7F;
        return bank | index;
    }
}
