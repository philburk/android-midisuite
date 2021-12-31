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

import android.util.Log;

import com.mobileer.miditools.midi20.tools.Midi;

public class CapabilityNegotiator {
    static final String TAG = "CapabilityNegotiator";

    private static final int STATE_IDLE = 0;
    private static final int STATE_INITIATOR_WAITING_REPLY = 1;
    private static final int STATE_RESPONDER_REPLIED = 2;
    private static final int STATE_INITIATOR_SWITCHING_PROTOCOL = 3;
    private static final int STATE_RESPONDER_WAITING_TEST = 4;
    private static final int STATE_INITIATOR_WAITING_TEST_REPLY = 5;
    private static final int STATE_RESPONDER_WAITING_CONFIRMATION = 6;
    private static final int STATE_FINISHED = 7;

    private static final int OPCODE_NONE = -1;
    private static final int INVALID_MNID = -1;
    private static final int TEST_DELAY_MSEC = 100;
    private static final int TEST_TIMEOUT_MSEC = 300;

    private int mSupportedVersion = Midi.VERSION_1_0;
    private int mPreviousVersion = Midi.VERSION_1_0;
    private int mNegotiatedVersion = Midi.VERSION_1_0;
    private int mNegotiationIdentifier = INVALID_MNID;
    private int mState = STATE_IDLE;
    private long mTime;
    private long mTimeStart;
    private boolean mInitiator = false;

    public void setSupportedVersion(int version) {
        mSupportedVersion = version;
    }

    public int getNegotiatedVersion() {
        return mNegotiatedVersion;
    }

    public boolean isIdle() {
        return mState == STATE_IDLE;
    }
    public boolean isFinished() {
        return mState == STATE_FINISHED;
    }

    public boolean isInitiator() {
        return mInitiator;
    }

    public void setInitiator(boolean mInitiator) {
        this.mInitiator = mInitiator;
    }

    public void setTime(long msec) {
        mTime = msec;
    }

    public String stateToString(int state) {
        String name = "INVALID";
        switch (state) {
            case STATE_IDLE:
                name = "IDLE";
                break;
            case STATE_INITIATOR_WAITING_REPLY:
                name = "INITIATOR_WAITING_REPLY";
                break;
            case STATE_RESPONDER_REPLIED:
                name = "RESPONDER_REPLIED";
                break;
            case STATE_INITIATOR_SWITCHING_PROTOCOL:
                name = "INITIATOR_SWITCHING_PROTOCOL";
                break;
            case STATE_RESPONDER_WAITING_TEST:
                name = "RESPONDER_WAITING_TEST";
                break;
            case STATE_INITIATOR_WAITING_TEST_REPLY:
                name = "INITIATOR_WAITING_TEST_REPLY";
                break;
            case STATE_RESPONDER_WAITING_CONFIRMATION:
                name = "RESPONDER_WAITING_CONFIRMATION";
                break;
            case STATE_FINISHED:
                name = "FINISHED";
                break;
            default:
                break;

        }
        return name;
    }

    public InquiryMessage advanceStateMachine(InquiryMessage inMessage) {
        InquiryMessage outMessage = null;

        int opcode = (inMessage == null) ? OPCODE_NONE : inMessage.getOpcode();

        //Log.d(TAG,"advanceStateMachine: state = " + stateToString(mState) + ", opcode = " + opcode);
        int beginningState = mState;
        switch(mState) {
            case STATE_IDLE:
                switch(opcode) {
                    case OPCODE_NONE:
                        if (mSupportedVersion == Midi.VERSION_2_0 && mInitiator) {
                            outMessage = new InquiryMessage(InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION);
                            mNegotiationIdentifier = InquiryMessage.generateNegotiationIdentifier();
                            outMessage.addProtocol(new ProtocolTypeMidiNew());
                            outMessage.addProtocol(new ProtocolTypeMidi10());
                            mState = STATE_INITIATOR_WAITING_REPLY;
                        }
                        break;
                    case InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION:
                        mNegotiationIdentifier = inMessage.getNegotiationIdentifier();
                        outMessage = new InquiryMessage(InquiryMessage.CI_SUBID2_REPLY_PROTOCOL_NEGOTIATION);
                        if (mSupportedVersion == Midi.VERSION_2_0) {
                            outMessage.addProtocol(new ProtocolTypeMidiNew());
                            outMessage.addProtocol(new ProtocolTypeMidi10());
                            mState = STATE_RESPONDER_REPLIED;
                        } else {
                            outMessage.addProtocol(new ProtocolTypeMidi10());
                            mState = STATE_FINISHED;
                        }
                        break;
                    default:
                        unexpectedMessage(inMessage);
                        break;
                }
                break;

            case STATE_INITIATOR_WAITING_REPLY:
                switch(opcode) {
                    case OPCODE_NONE:
                        break;
                    case InquiryMessage.CI_SUBID2_INITIATE_PROTOCOL_NEGOTIATION:
                        if (mNegotiationIdentifier == inMessage.getNegotiationIdentifier()) {
                            // TODO Collision
                        }
                        break;
                    case InquiryMessage.CI_SUBID2_REPLY_PROTOCOL_NEGOTIATION:
                        if (mNegotiationIdentifier == inMessage.getNegotiationIdentifier()) {
                            if (inMessage.supportsMidi20()) {
                                outMessage = new InquiryMessage(InquiryMessage.CI_SUBID2_SET_PROTOCOL);
                                outMessage.addProtocol(new ProtocolTypeMidiNew());
                                mTimeStart = mTime;
                                mPreviousVersion = mNegotiatedVersion;
                                mNegotiatedVersion = Midi.VERSION_2_0;
                                mState = STATE_INITIATOR_SWITCHING_PROTOCOL;
                            } else {
                                mState = STATE_FINISHED;
                            }
                        }
                        break;
                    default:
                        unexpectedMessage(inMessage);
                        break;
                }
                break;

            case STATE_INITIATOR_SWITCHING_PROTOCOL:
                if (mTime >= (mTimeStart + TEST_DELAY_MSEC)) {
                    outMessage = new InquiryMessage(InquiryMessage.CI_SUBID2_TEST_INITIATOR_TO_RESPONDER);
                    mState = STATE_INITIATOR_WAITING_TEST_REPLY;
                    mTimeStart = mTime;
                }
                break;

            case STATE_RESPONDER_REPLIED:
                switch(opcode) {
                    case OPCODE_NONE:
                        break;
                    case InquiryMessage.CI_SUBID2_SET_PROTOCOL:
                        if (mNegotiationIdentifier == inMessage.getNegotiationIdentifier()) {
                            mNegotiatedVersion = inMessage.supportsMidi20()
                                                 ? Midi.VERSION_2_0
                                                 : Midi.VERSION_1_0;
                            mState = STATE_RESPONDER_WAITING_TEST;
                        } else {
                            unexpectedMessage(inMessage);
                        }
                        mTimeStart = mTime;
                        break;
                    default:
                        unexpectedMessage(inMessage);
                        break;
                }
                break;

            case STATE_RESPONDER_WAITING_TEST:
                switch(opcode) {
                    case OPCODE_NONE:
                        if (mTime >= (mTimeStart + TEST_TIMEOUT_MSEC)) {
                            // Timeout
                            Log.i(TAG,"pollMessage() - timeout in STATE_RESPONDER_WAITING_TEST");
                            mNegotiatedVersion = Midi.VERSION_1_0;
                            mState = STATE_IDLE;
                        }
                        break;
                    case InquiryMessage.CI_SUBID2_TEST_INITIATOR_TO_RESPONDER:
                        if (mNegotiationIdentifier == inMessage.getNegotiationIdentifier()) {
                            outMessage = new InquiryMessage(InquiryMessage.CI_SUBID2_TEST_RESPONDER_TO_INITIATOR);
                            mState = STATE_RESPONDER_WAITING_CONFIRMATION;
                            mTimeStart = mTime;
                        } else {
                            unexpectedMessage(inMessage);
                        }
                        break;
                    default:
                        unexpectedMessage(inMessage);
                        break;
                }
                break;

            case STATE_INITIATOR_WAITING_TEST_REPLY:
                switch(opcode) {
                    case OPCODE_NONE:
                        break;
                    case InquiryMessage.CI_SUBID2_TEST_RESPONDER_TO_INITIATOR:
                        if (mNegotiationIdentifier == inMessage.getNegotiationIdentifier()) {
                            outMessage = new InquiryMessage(InquiryMessage.CI_SUBID2_CONFIRM_NEW_PROTOCOL);
                            mState = STATE_FINISHED;
                        } else {
                            // TODO Collision
                        }
                        mTimeStart = mTime;
                        break;
                    default:
                        unexpectedMessage(inMessage);
                        break;
                }
                break;

            case STATE_RESPONDER_WAITING_CONFIRMATION:
                switch(opcode) {
                    case OPCODE_NONE:
                        break;
                    case InquiryMessage.CI_SUBID2_CONFIRM_NEW_PROTOCOL:
                        if (mNegotiationIdentifier == inMessage.getNegotiationIdentifier()) {
                            mState = STATE_FINISHED;
                        } else {
                            unexpectedMessage(inMessage);
                        }
                        mTimeStart = mTime;
                        break;
                    default:
                        unexpectedMessage(inMessage);
                        break;
                }
                break;

            case STATE_FINISHED:
                break;

            default:
                Log.i(TAG,"advanceStateMachine() - state not handled! " + mState);
                break;
        }

        if (outMessage != null) {
            outMessage.setNegotiationIdentifier(mNegotiationIdentifier);
        }

        if (mState != beginningState) {
            System.out.println("advanceStateMachine() - state transition from "
                    + stateToString(beginningState)
                    + " to " + stateToString(mState)
                    + ", opcode = " + opcode);
        }
        return outMessage;
    }

    private void unexpectedMessage(InquiryMessage message) {
        System.out.println("unexpectedMessage() - mState = " + mState + ", msg = " + message);
        mState = STATE_IDLE;
    }
}
