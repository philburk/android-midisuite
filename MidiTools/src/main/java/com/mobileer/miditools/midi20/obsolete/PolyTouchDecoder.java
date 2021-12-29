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

import android.util.Log;

import com.mobileer.miditools.midi20.protocol.MidiPacketBase;
import com.mobileer.miditools.midi20.protocol.PacketDecoder;

public class PolyTouchDecoder extends PolyTouchWrapper implements PacketDecoder {
    static final String TAG = "PolyTouchDecoder";
    public final static int STATE_IDLE = 0;
    public final static int STATE_GOT_STATUS = 1;
    public final static int STATE_GOT_DATA1 = 2;
    private int state = STATE_IDLE;
    private int status;
    private int data1;
    private int halfWordCount = 0;

    @Override
    public boolean decode(byte[] stream, int offset, int len, MidiPacketBase packet) {
        boolean done = false;
        int cursor = offset;
        for (int i = 0; i < len && !done; i++) {
            int b = stream[cursor] & 0x0FF;
            //System.out.printf("decode: b = 0x%02X, state = %d\n", b, state);

            switch (state) {
                case STATE_IDLE:
                    if ((b & TOUCH_OPCODE_MASK) == TOUCH_OPCODE) {
                        status = b;
                        state = STATE_GOT_STATUS;
                    }
                    cursor++;
                    break;

                case STATE_GOT_STATUS:
                    if ((b & 0x080) == 0) {
                        data1 = b;
                        state = STATE_GOT_DATA1;
                        cursor++;
                    } else {
                        state =  STATE_IDLE; // look again
                    }
                    break;

                case STATE_GOT_DATA1:
                    if ((b & 0x080) == 0) {
                        int data2 = b;
                        int halfWord = decodeHalfWord(status, data1, data2);
                        int wordCount = halfWordCount / 2;
                        if ((halfWordCount & 1) == 0) {
                            packet.setWord(wordCount, (long) (halfWord << 16));
                        } else {
                            packet.setWord(wordCount, packet.getWord(wordCount) | halfWord);
                        }
                        Log.i(TAG, "GOT_DATA1, halfWordCount = "
                                + halfWordCount + ", packet = " + packet);
                        halfWordCount++;
                        int touchType = status & TOUCH_TYPE_MASK;
                        done = (touchType == TOUCH_TYPE_END);
                        cursor++;
                    }
                    state = STATE_IDLE;
                    break;

                default:
                    state = STATE_IDLE;
                    break;
            }
        }

        return done;
    }

    public static int decodeHalfWord(int status, int d1, int d2) {
        int halfWord = (status & 0x02) << 14;
        halfWord |= (status & 0x01) << 7;
        halfWord |= d1 << 8;
        halfWord |= d2;
        Log.i(TAG, String.format("halfWord = 0x%04X", halfWord));
        return halfWord;
    }
}
