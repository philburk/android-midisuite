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

/**
 * Definition for the MIDI 2.0 packet.
 *
 * A packet is defined as a block of 1-4 32-bit words.
 * The packet has 16 Message Types, which include messages
 * equivalent to all of the MIDI 1.0 message.
 * This packet can also encode many new messages including
 * PerNoteControlChange, NoteOns with pitch, etc.
 */

/*
TODO Add support for SysEx data packets.
TODO Negotiation should switch to UMP for SysEx test.
TODO Fill in missing opcodes.
TODO add more tests.
TODO Review APIs.
TODO Add USB-MIDI 2.0 support to Scope.
TODO Review
 */
public class UniversalMidiPacket {
    /**
     * The number of words in a packet for each Message Type.
     */
    private final static int[] PACKET_LENGTHS = {1, 1, 1, 2, 2, 4, 1, 1, 2, 2, 2, 3, 3, 4, 4, 4};

    public static final int TYPE_UTILITY = 0;
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_CHANNEL_VOICE_M1 = 2; // MIDI 1.0
    public static final int TYPE_DATA_64 = 3;
    public static final int TYPE_CHANNEL_VOICE_M2 = 4; // MIDI 2.0
    public static final int TYPE_DATA_128 = 5;
    // Higher Message Types are reserved, as of January 15, 2022.

    // These opcodes are new to MIDI 2.0.
    public static final int OPCODE_PER_NOTE_RPN = 0x0;
    public static final int OPCODE_PER_NOTE_NRPN = 0x1;
    public static final int OPCODE_RPN = 0x2;
    public static final int OPCODE_NRPN = 0x3;
    public static final int OPCODE_RELATIVE_RPN = 0x4;
    public static final int OPCODE_RELATIVE_NRPN = 0x5;
    public static final int OPCODE_PER_NOTE_PITCH_BEND = 0x6;

    // These opcodes are easily translatable to MIDI 1.0 messages.
    public static final int OPCODE_NOTE_OFF = 0x8;
    public static final int OPCODE_NOTE_ON = 0x9;
    public static final int OPCODE_POLY_PRESSURE = 0xA;
    public static final int OPCODE_CONTROL_CHANGE = 0xB;
    public static final int OPCODE_PROGRAM_CHANGE = 0xC;
    public static final int OPCODE_CHANNEL_PRESSURE = 0xD;
    public static final int OPCODE_PITCH_BEND = 0xE;
    
    public static final int OPCODE_PER_NOTE_MANAGEMENT = 0xF;

    public static final int FLAG_PROGRAM_CHANGE_BANK_VALID = 0x00000001;

    public static final int STATUS_SYSEX_COMPLETE = 0;
    public static final int STATUS_SYSEX_START = 1;
    public static final int STATUS_SYSEX_CONTINUE = 2;
    public static final int STATUS_SYSEX_END = 3;

    private final int[] data = new int[4];

    public int wordCount() {
        return PACKET_LENGTHS[getType()];
    }

//    public static int wordCount(int firstWord) {
//        return PACKET_LENGTHS[(firstWord >> 28) & 0x0F];
//    }

    @Override
    public boolean equals(Object other) {
        if (other instanceof UniversalMidiPacket) {
            UniversalMidiPacket packet = (UniversalMidiPacket) other;
            if (data[0] != packet.data[0]) return false;
            int wordCount = wordCount();
            if (wordCount >= 2) {
                if (data[1] != packet.data[1]) return false;
                if (wordCount >= 3) {
                    if (data[2] != packet.data[2]) return false;
                    if (wordCount >= 4) {
                        if (data[3] != packet.data[3]) return false;
                    }
                }
            }
            return true;
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

    public static UniversalMidiPacket create() {
        return new UniversalMidiPacket();
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

    /**
     *
     * @param value unsigned integer ranging from 0 to 0x0FFFF
     */
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

    /**
     *
     * @param opcode
     * @param noteNumber ranging from 0 to 127
     * @param velocity ranging from 0 to 0x0FFFF
     */
    protected void setupNote(int opcode, int noteNumber, int velocity) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(opcode);
        setHeaderByte(noteNumber, 8);
        setData1MSW(velocity);
    }

    /**
     * Turn ON a note.
     * @param noteNumber ranging from 0 to 127
     * @param velocity ranging from 0 to 0x0FFFF
     */
    public void noteOn(int noteNumber, int velocity) {
        setupNote(OPCODE_NOTE_ON, noteNumber, velocity);
    }

    /**
     * Turn OFF a note.
     * @param noteNumber ranging from 0 to 127
     * @param velocity ranging from 0 to 0x0FFFF
     */
    public void noteOff(int noteNumber, int velocity) {
        setupNote(OPCODE_NOTE_OFF, noteNumber, velocity);
    }

    public void programChange(int program) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_PROGRAM_CHANGE);
        setProgram(program);
    }

    public void programChange(int program, int bank) {
        programChange(program);
        setBank(bank);
    }

    /**
     * Configure the packet as a Control Change message.
     * @param index ranging from 0 to 127
     * @param value a normalized value between 0.0 and 1.0
     */
    public void controlChange(int index, double value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_CONTROL_CHANGE);
        setHeaderL77(index);
        setNormalizedControllerValue(value);
    }

    /**
     * Configure the packet as a Control Change message.
     * @param index ranging from 0 to 127
     * @param value ranging from 0 to 0x0FFFFFFFF
     */
    public void controlChange(int index, long value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_CONTROL_CHANGE);
        setHeaderL77(index);
        data[1] = (int) value;
    }

    /**
     * Configure the packet as a Registered Controller message.
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from 0.0 and 1.0
     */
    public void RPN(int index, double value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_RPN);
        setHeaderL77(index);
        setNormalizedControllerValue(value);
    }

    /**
     * Configure the packet as an Registered Controller
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from 0 to 0x0FFFFFFFF
     */
    public void RPN(int index, long value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_RPN);
        setHeaderL77(index);
        data[1] = (int) value;
    }

    /**
     * Configure the packet as an Assignable Controller message.
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from 0.0 and 1.0
     */
    public void NRPN(int index, double value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_RPN);
        setHeaderL77(index);
        setNormalizedControllerValue(value);
    }

    /**
     * Configure the packet as an Assignable Controller
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from 0 to 0x0FFFFFFFF
     */
    public void NRPN(int index, long value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_NRPN);
        setHeaderL77(index);
        data[1] = (int) value;
    }

    /**
     * Configure the packet as a Registered Controller message.
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from 0.0 and 1.0
     */
    public void relativeRPN(int index, double value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_RPN);
        setHeaderL77(index);
        setRelativeControllerValue(value);
    }

    /**
     * Configure the packet as an Registered Controller
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from Integer.MIN to Integer.MAX
     */
    public void relativeRPN(int index, int value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_RELATIVE_RPN);
        setHeaderL77(index);
        data[1] = (int) value;
    }

    /**
     * Configure the packet as an Assignable Controller message.
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from -1.0 and 1.0
     */
    public void relativeNRPN(int index, double value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_RELATIVE_NRPN);
        setHeaderL77(index);
        setRelativeControllerValue(value);
    }

    /**
     * Configure the packet as an Assignable Controller
     * @param index ranging from 0 to 0x3FFF, includes bank
     * @param value ranging from Integer.MIN_VALUE to Integer.MAX_VALUE
     */
    public void relativeNRPN(int index, int value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_RELATIVE_NRPN);
        setHeaderL77(index);
        data[1] = value;
    }

    /**
     * Configure the packet as a Per-Note Registered Controller
     * @param noteNumber ranging from 0 to 127
     * @param index ranging from 0 to 255
     * @param value ranging from 0 to 0x0FFFFFFFF
     */
    public void perNoteRPN(int noteNumber, int index, int value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_PER_NOTE_RPN);
        setNotePlus(noteNumber, index);
        data[1] = value;
    }

    /**
     * Configure the packet as a Per-Note Assignable Controller
     * @param noteNumber ranging from 0 to 127
     * @param index ranging from 0 to 255
     * @param value ranging from 0 to 0x0FFFFFFFF
     */
    public void perNoteNRPN(int noteNumber, int index, int value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_PER_NOTE_NRPN);
        setNotePlus(noteNumber, index);
        data[1] = value;
    }

    /**
     * Configure the packet as a Pitch Bend
     * @param value unsigned bipolar value ranging from 0 to 0x0FFFFFFFF
     */
    public void pitchBend(long value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_PITCH_BEND);
        data[1] = (int) value;
    }

    /**
     * Configure the packet as a Per-Note Pitch Bend
     * @param noteNumber ranging from 0 to 127
     * @param value unsigned bipolar value ranging from 0 to 0x0FFFFFFFF
     */
    public void perNotePitchBend(int noteNumber, long value) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_PER_NOTE_PITCH_BEND);
        setNoteNumber(noteNumber);
        data[1] = (int) value;
    }

    /**
     * Configure the packet as a Per-Note Management message
     * @param noteNumber ranging from 0 to 127
     * @param detach - if true, detach controller from currently active notes
     * @param reset - reset per-note controllers to their default values
     */
    public void perNoteManagement(int noteNumber, boolean detach, boolean reset) {
        setType(TYPE_CHANNEL_VOICE_M2);
        setOpcode(OPCODE_PER_NOTE_PITCH_BEND);
        int flags = reset ? 1 : 0;
        flags += detach ? 2 : 0;
        setNotePlus(noteNumber, flags);
        data[1] = 0; // reserved
    }

    public int getControllerIndex() {
        return getHeaderL77();
    }

    /**
     *
     * @param group
     * @param status
     * @param payload values must be between 0 and 127
     * @param offset
     * @param count
     */
    public void systemExclusive7(int group, int status, byte[] payload, int offset, int count) {
        int word = (TYPE_DATA_64 << 28)
                | (group << 24)
                | (status << 20)
                | (count << 16);
        setHeader(word);
        if (count == 0) return;
        data[0] |= payload[offset++] << 8;
        if (count == 1) return;
        data[0] |= payload[offset++];
        if (count == 2) return;
        data[1] |= payload[offset++] << 24;
        if (count == 3) return;
        data[1] |= payload[offset++] << 16;
        if (count == 4) return;
        data[1] |= payload[offset++] << 8;
        if (count == 5) return;
        data[1] |= payload[offset];
    }

    /**
     *
     * @param group
     * @param status
     * @param streamId
     * @param payload
     * @param offset
     * @param count
     */
    public void systemExclusive8(int group, int status, int streamId,
                                 byte[] payload, int offset, int count) {
        int word = (TYPE_DATA_128 << 28)
                | (group << 24)
                | (status << 20)
                | (count << 16)
                | (streamId << 8);
        setHeader(word);
        // Encode payload bytes into remaining words.
        int wordIndex = 0;
        int shifter = 0;
        while(count > 0) {
            data[wordIndex] |= (int)((payload[offset++] & 0xFF) << shifter);
            shifter -= 8;
            if (shifter < 0) {
                shifter = 24;
                wordIndex++;
            }
            count--;
        }
    }

    static UniversalMidiPacket[] encodeMultipleSysex7(int group,
                                                      byte[] payload, int offset, int count) {
        final int maxPerPacket = 6;
        if (count <= maxPerPacket) {
            UniversalMidiPacket packet = new UniversalMidiPacket();
            packet.systemExclusive7(group, STATUS_SYSEX_COMPLETE,
                    payload, offset, count);
            return new UniversalMidiPacket[]{ packet };
        } else {
            int numPackets = (count + maxPerPacket - 1) / maxPerPacket;
            UniversalMidiPacket[] packets = new UniversalMidiPacket[numPackets];
            int packetIndex = 0;
            while (count > 0) {
                int status = (packetIndex == 0) ? STATUS_SYSEX_START
                        : (count <= maxPerPacket) ? STATUS_SYSEX_END : STATUS_SYSEX_CONTINUE;
                UniversalMidiPacket packet = new UniversalMidiPacket();
                int currentCount = Math.min(maxPerPacket, count);
                packet.systemExclusive7(group, status,
                        payload, offset, currentCount);
                offset += currentCount;
                count -= currentCount;
                packets[packetIndex++] = packet;
            }
            return packets;
        }
    }

    static UniversalMidiPacket[] encodeMultipleSysex8(int group, int streamId,
                                                             byte[] payload, int offset, int count) {
        final int maxPerPacket = 13;
        if (count <= maxPerPacket) {
            UniversalMidiPacket packet = new UniversalMidiPacket();
            packet.systemExclusive8(group, STATUS_SYSEX_COMPLETE, streamId,
                    payload, offset, count);
            return new UniversalMidiPacket[]{ packet };
        } else {
            int numPackets = (count + maxPerPacket - 1) / maxPerPacket;
            UniversalMidiPacket[] packets = new UniversalMidiPacket[numPackets];
            int packetIndex = 0;
            while (count > 0) {
                int status = (packetIndex == 0) ? STATUS_SYSEX_START
                        : (count <= maxPerPacket) ? STATUS_SYSEX_END : STATUS_SYSEX_CONTINUE;
                UniversalMidiPacket packet = new UniversalMidiPacket();
                int currentCount = Math.min(maxPerPacket, count);
                packet.systemExclusive8(group, status, streamId,
                        payload, offset, currentCount);
                offset += currentCount;
                count -= currentCount;
                packets[packetIndex++] = packet;
            }
            return packets;
        }
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

    /**
     *
     * @param noteNumber ranging from 0 to 127
     * @param other ranging from 0 to 255
     */
    private void setNotePlus(int noteNumber, int other) {
        int header = getHeader();
        header &= 0xFFFF8000; // make hole
        header |= (noteNumber & 0x7F) << 8;
        header |= other & 0xFF; // 8-bit index
        setHeader(header);
    }

    /**
     *
     * @param noteNumber ranging from 0 to 127
     */
    protected void setNoteNumber(int noteNumber) {
        int header = getHeader();
        header &= 0xFFFF80FF; // make hole
        header |= (noteNumber & 0x7F) << 8;
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

    /**
     * Scale and clip value to fit in 32-bit unsigned data field.
     * @param value between -1.0 and +1.0
     */
    public void setNormalizedControllerValue(double value) {
        long scaled = (long)(value * (1L << 32));
        data[1] = (int) (Math.min(0x0FFFFFFFFL,
                Math.max(0, scaled)));
    }

    /**
     *
     * @param value between -1.0 and +1.0
     */
    public void setRelativeControllerValue(double value) {
        long scaled = (long)(value * Integer.MAX_VALUE);
        data[1] = (int) (Math.min(Integer.MAX_VALUE,
                Math.max(Integer.MIN_VALUE, scaled)));
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
