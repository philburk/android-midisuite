package com.mobileer.miditools;

import android.media.midi.MidiReceiver;

import java.io.IOException;

public class SmartMidiReceiver extends MidiReceiver {

    MidiReceiver mOtherReceiver;

    public MidiReceiver getOtherReceiver() {
        return mOtherReceiver;
    }

    public void setOtherReceiver(MidiReceiver otherReceiver) {
        this.mOtherReceiver = otherReceiver;
    }

    // Handle messages sent TO this receiver.
    @Override
    public void onSend(byte[] data, int offset, int numBytes, long timestamp) throws IOException {
        // TODO CI negotiation
    }

    public void reset() {

    }

}
