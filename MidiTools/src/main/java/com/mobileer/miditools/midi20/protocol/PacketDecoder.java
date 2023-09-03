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

public interface PacketDecoder {

    /**
     * Prepare the Decoder to read packets from a region in a byte array.
     * @param data
     * @param offset
     * @param length
     */
    void wrap(byte[] data, int offset, int length);

    /**
     * Decode the next packet from the byte array.
     * It will return false when there are no more packets.
     *
     * TODO Consider returning a new packet or null.
     *
     * @param packet
     * @return true if the packet was fully decoded
     */
    boolean decode(UniversalMidiPacket packet);
}
