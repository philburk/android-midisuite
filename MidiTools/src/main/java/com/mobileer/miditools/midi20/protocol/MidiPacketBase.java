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

public abstract class MidiPacketBase {
    private final static int[] PACKET_LENGTHS = {1, 1, 1, 2, 2, 4, 1, 1, 2, 2, 2, 3, 3, 4, 4, 4};

    public static final int TYPE_UTILITY = 0;
    public static final int TYPE_SYSTEM = 1;
    public static final int TYPE_CHANNEL_VOICE = 2; // MIDI 1.0
    public static final int TYPE_DATA_64 = 3;
    public static final int TYPE_CHANNEL_VOICE_HD = 4;
    public static final int TYPE_DATA_128 = 5;

    protected int[] data = new int[4];

    public int wordCount() {
        return PACKET_LENGTHS[getType()];
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
        return new HDMidiPacket();
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

    public abstract void noteOn(int noteNumber, int velocity);

    public abstract boolean isNoteOn();

    public abstract void noteOff(int noteNumber, int velocity);

    public abstract boolean isNoteOff();

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
        data[1] = (data[1] & 0xFFFF0000)
                | ((bank << 1) & 0x7F00)
                | (bank & 0x007F);
    }

    public int getBank() {
        int bank = (data[1] & 0x7F00) >> 1;
        bank |= data[1] & 0x07F;
        return bank;
    }

    public abstract void programChange(int bank, int program);

    /**
     *
     * @param index
     * @param value normalized [0.0, 1.0)
     */
    public abstract void controlChange(int index, double value);

    public abstract void controlChange(int index, long value);

    public abstract boolean isControlChange();

    public abstract int getControllerIndex();


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
