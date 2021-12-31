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

public class MidiPacketBase {
    private final static int[] PACKET_LENGTHS = {1, 1, 1, 2, 2, 4, 1, 1, 2, 2, 2, 3, 3, 4, 4, 4};

    public static final int TYPE_UTILITY = 0;
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_CHANNEL_VOICE = 2; // MIDI 1.0
    public static final int TYPE_DATA_64 = 3;
    public static final int TYPE_CHANNEL_VOICE_HD = 4;
    public static final int TYPE_DATA_128 = 5;

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

    public static final int FLAG_PROGRAM_CHANGE_BANK_VALID = 0x00000001;



    protected int[] data = new int[4];

    public int wordCount() {
        return PACKET_LENGTHS[getType()];
    }

    public static int wordCount(int firstWord) {
        return PACKET_LENGTHS[(firstWord >> 28) & 0x0F];
    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof MidiPacketBase) {
            MidiPacketBase packet = (MidiPacketBase) other;
            int wordCount = wordCount();
            if (wordCount >= 1 && data[0] != packet.data[0]) return false;
            else if (wordCount >= 2 && data[1] != packet.data[1]) return false;
            else if (wordCount >= 3 && data[2] != packet.data[2]) return false;
            else if (wordCount >= 4 && data[3] != packet.data[3]) return false;
            else return true;
        }
        else return false;
    }

    @Override
    public int hashCode() {
        int wordCount = wordCount();
        int hash = data[0];
        if (wordCount > 1) {
            hash ^= data[1];
            if (wordCount > 2) {
                hash ^= data[2];
                if (wordCount > 3) {
                    hash ^= data[3];
                }
            }
        }
        return hash;
    }

    public static MidiPacketBase create() {
        return new MidiPacketBase();
    }

    protected int getHeader() {
        return data[0];
    }

    protected void setHeader(int header) {
        data[0] = header;
    }

    /**
     * @return type between 0 and 15
     */
    public int getType() {
        return (getHeader() >> 28) & 0x0F;
    }

    /**
     * Only valid for channel voice messages.
     * @return group between 0 and 15
     */
    public int getGroup() {
        return (getHeader() >> 24) & 0x0F;
    }

    /**
     * @return opcode between 0 and 15
     */
    public int getOpcode() {
        return (getHeader() >> 20) & 0x0F;
    }

    /**
     * Only valid for channel voice messages.
     * @return channel between 0 and 15
     */
    public int getChannel() {
        return (getHeader() >> 16) & 0x0F;
    }

    protected void setHeaderNibble(int value, int shift) {
        int header = getHeader();
        header &= ~(0xF << shift);  // make hole
        header |= value << shift;  // fill hole
        setHeader(header);
    }

    protected void setHeaderByte(int value, int shift) {
        int header = getHeader();
        header &= ~(0xFF << shift); // make hole
        header |= value << shift; // fill hole
        setHeader(header);
    }

    protected void setHeaderLSW(int index) {
        int header = getHeader();
        header &= 0xFFFF0000; // make hole
        header |= index; // fill hole
        setHeader(header);
    }

    protected void setData1MSW(int value) {
        int n = data[1];
        n &= ~(0xFFFF << 16); // make hole
        n |= value << 16; // fill hole
        data[1] = n;
    }

    public void setType(int type) {
        setHeaderNibble(type, 28);
    }

    public void setGroup(int group) {
        setHeaderNibble(group, 24);
    }

    public void setOpcode(int opcode) {
        setHeaderNibble(opcode, 20);
    }

    public void setChannel(int channel) {
        setHeaderNibble(channel, 16);
    }


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

    public void programChange(int program) {
        setType(TYPE_CHANNEL_VOICE_HD);
        setOpcode(OPCODE_PROGRAM_CHANGE);
        setProgram(program);
    }

    public void programChange(int program, int bank) {
        programChange(program);
        setBank(bank);
    }

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

    public int getNoteNumber() {
        return (getHeader() >> 8) & 0x0FF;
    }

    public int getVelocity() {
        return (data[1] >> 16) & 0x0FFFF;
    }

    public void setProgram(int program) {
        data[1] = (data[1] & 0x00FFFFFF) | ((program & 0x07F) << 24);
    }

    public int getProgram() {
        return (data[1] >> 24) & 0x07F;
    }

    public void setBank(int bank) {
        data[0] |= FLAG_PROGRAM_CHANGE_BANK_VALID;
        data[1] = (data[1] & 0xFFFF0000)
                | ((bank << 1) & 0x7F00)
                | (bank & 0x007F);
    }

    public int getBank() {
        if ((data[0] & FLAG_PROGRAM_CHANGE_BANK_VALID) == 0) {
            return 0;
        }
        int bank = (data[1] & 0x7F00) >> 1;
        bank |= data[1] & 0x07F;
        return bank;
    }

    public long getControllerValue() {
        return 0x0FFFFFFFFL & (long) data[1];
    }

    public double getNormalizedControllerValue() {
        return getControllerValue() * (1.0 / (1L << 32));
    }

    public void setNormalizedControllerValue(double value) {
        long big = (0x0FFFFFFFFL & (long)(value * (1L << 32)));
        data[1] = (int) (0x0FFFFFFFFL & (long)(value * (1L << 32)));
    }

    public long getWord(int i) {
        return data[i];
    }
    public void setWord(int i, long n) {
        data[i] = (int) n;
    }

    @Override
    public String toString() {
        String text = "";
        int wordCount = wordCount();
        for (int i = 0; i < wordCount; i++) {
            text += String.format("0x%08X, ", data[i]);
        }
        return text;
    }
}
