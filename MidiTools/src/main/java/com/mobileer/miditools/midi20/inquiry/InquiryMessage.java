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

import com.mobileer.miditools.midi20.tools.Midi;
import com.mobileer.miditools.midi20.tools.MidiReader;
import com.mobileer.miditools.midi20.tools.MidiWriter;

import java.io.IOException;
import java.util.ArrayList;


public class InquiryMessage extends Midi {

    private int mOpcode;
    private int mNegotiationIdentifier; // MNID Negotiation Identifier
    private int mAuthorityLevel;
    private int mDeviceId = CI_TOFROM_PORT;
    private int mVersion = CI_VERSION;
    private int mManufacturer = 0x00020D; // 3 bytes, Google MMA ID, FIXME - set from app
    private int mFamily; // two 7-bit bytes, eg. 0x0173
    private int mModel; // 14 bits
    private int mRevision; // 28 bits
    private boolean mSupportsMidi20;

    private ArrayList<ProtocolType> mProtocols = new ArrayList<ProtocolType>();

    public InquiryMessage(int opcode) {
        mOpcode = opcode;
    }

    public InquiryMessage() {
    }

    public static int generateNegotiationIdentifier() {
        return (int) (Math.random() * (1 << 28)); // 28-bit MNID
    }

    public void addProtocol(ProtocolType protocolType) {
        mProtocols.add(protocolType);
        mSupportsMidi20 |= protocolType.getType() == ProtocolType.TYPE_MIDI_NEW; // TODO Check Version
    }

    public boolean supportsMidi20() {
        return mSupportsMidi20;
    }

    public int encode(MidiWriter buffer) {
        buffer.write(SYSEX_START);
        encodePayload(buffer);
        buffer.write(Midi.SYSEX_END);
        return buffer.getCursor();
    }

    public void encodePayload(MidiWriter buffer) {
        buffer.write(SYSEX_UNIVERSAL);
        buffer.write(mDeviceId);
        buffer.write(SYSEX_SUBID1_CI);
        buffer.write(mOpcode);
        buffer.write(mVersion);
        buffer.write28bits(mNegotiationIdentifier);
        buffer.write(mAuthorityLevel);
        switch(mOpcode) {
            case CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION:
            case CI_SUBID2_REPLY_PROTOCOL_NEGOTIATION:
            case CI_SUBID2_SET_PROTOCOL:
                buffer.writeManufacturerId(mManufacturer);
                buffer.write2(mFamily);
                buffer.write2(mModel);
                buffer.write3(mRevision);
                buffer.write(mProtocols.size());
                for (ProtocolType protocolType : mProtocols) {
                    protocolType.encode(buffer);
                }
                break;

            case CI_SUBID2_TEST_INITIATOR_TO_RESPONDER:
            case CI_SUBID2_TEST_RESPONDER_TO_INITIATOR:
                for (int i = 0; i < 0x30; i++) {
                    buffer.write(i);
                }
                break;

            case CI_SUBID2_CONFIRM_NEW_PROTOCOL:
                break;
        }
    }

    // TODO Abort if status byte read, except real-time.
    public int decode(MidiReader reader) throws IOException {
        if ((reader.read() != SYSEX_START)
                || (reader.read() != SYSEX_UNIVERSAL)) throw new IOException("Not Universal SysEx");
        decodePayload(reader);
        if (reader.read() != SYSEX_END) throw new IOException("SysEx End Missing");
        return reader.getCursor();
    }

    protected int decodePayload(MidiReader reader) throws IOException {
        mDeviceId = reader.read(); // CI_TOFROM_PORT;
        if (reader.read() != SYSEX_SUBID1_CI) throw new IOException("SysEx not CI");
        mOpcode = reader.read();
        mVersion = reader.read();
        mNegotiationIdentifier = reader.read28bits();
        mAuthorityLevel = reader.read();

        // This part of the payload depends on the opcode (subId2)
        switch(mOpcode) {
            case CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION:
            case CI_SUBID2_REPLY_PROTOCOL_NEGOTIATION:
            case CI_SUBID2_SET_PROTOCOL:
                mManufacturer = reader.readManufacturerId();
                mFamily = reader.read2(); // TODO Little Endian
                mModel = reader.read2();  // TODO Little Endian
                mRevision = reader.read3();
                int numProtocols = reader.read();
                for (int i = 0; i < numProtocols; i++) {
                    int type = reader.peek();
                    ProtocolType protocolType = ProtocolType.create(type);
                    protocolType.decode(reader);
                    addProtocol(protocolType);
                }
                break;

            case CI_SUBID2_TEST_INITIATOR_TO_RESPONDER:
            case CI_SUBID2_TEST_RESPONDER_TO_INITIATOR:
                for (int i = 0; i < 0x30; i++) {
                    int actual = reader.read();
                    if (actual != i) {
                        throw new IOException("CI Test sequence contains "
                                + actual + ", expected " + i);
                    }
                }
                break;

            case CI_SUBID2_CONFIRM_NEW_PROTOCOL:
                break;
        }

        return reader.getCursor();
    }

    public int getOpcode() {
        return mOpcode;
    }

    public void setOpcode(int opcode) {
        mOpcode = opcode;
    }

    public int getNegotiationIdentifier() {
        return mNegotiationIdentifier;
    }

    public void setNegotiationIdentifier(int identifier) {
        mNegotiationIdentifier = identifier;
    }

    public String opcodeToString(int opcode) {
        String text;
        switch(opcode) {
            case CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION:
                text = "Initiate";
                break;
            case CI_SUBID2_REPLY_PROTOCOL_NEGOTIATION:
                text = "Reply";
                break;
            case CI_SUBID2_SET_PROTOCOL:
                text = "SetProtocol";
                break;
            case CI_SUBID2_TEST_INITIATOR_TO_RESPONDER:
                text = "TestItoR";
                break;
            case CI_SUBID2_TEST_RESPONDER_TO_INITIATOR:
                text = "TestRtoI";
                break;
            case CI_SUBID2_CONFIRM_NEW_PROTOCOL:
                text = "Confirm";
                break;
            default:
                text = "unknown";
                break;
        }
        return text;
    }

    @Override
    public String toString() {
        return "CI: opcode = " + opcodeToString(mOpcode);
    }
}
