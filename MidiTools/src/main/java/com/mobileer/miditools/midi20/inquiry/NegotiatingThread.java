package com.mobileer.miditools.midi20.inquiry;

import android.media.midi.MidiReceiver;
import android.util.Log;

import com.mobileer.miditools.midi20.tools.Midi;
import com.mobileer.miditools.midi20.tools.MidiReader;
import com.mobileer.miditools.midi20.tools.MidiWriter;

import java.io.IOException;

/**
 * A thread that negotiates the transfer of MIDI 2.0 Universal MIDI Packets.
 */
public class NegotiatingThread extends MidiReceiver implements Runnable {
    static final String TAG = "NegotiatingThread";
    private static final long PERIOD_MSEC = 100;

    private CapabilityNegotiator mNegotiator;
    private MidiReceiver mTargetReceiver;
    private Thread mThread;
    private volatile boolean mRunning = false;
    private static final boolean mEnabled = true;
    private boolean mInitiator = false;

    public static boolean isEnabled() {
        return mEnabled;
    }

    @Override
    public void onSend(byte[] bytes, int offset, int count, long timestamp) throws IOException {
//        Log.i(TAG,"onSend(byte = " + bytes[offset]
//                + ", offset = " + offset
//                + ", count = " + count + ")");
        if ((bytes[offset] & 0x0FF) == Midi.SYSEX_START) {
            // Log.i(TAG,"onSend() got SYSEX_START");
            InquiryMessage message = new InquiryMessage();
            MidiReader reader = new MidiReader(bytes, offset); // ignores count!
            boolean valid = false;
            try {
                message.decode(reader);
                valid = true;
            } catch( IOException e) {
            }
            if (valid) {
                handleMessage(message);
            }
        }
    }

    public void setTargetReceiver(MidiReceiver receiver) {
        mTargetReceiver = receiver;
    }

    private synchronized void handleMessage(InquiryMessage message) throws IOException {
        if (message != null) {
            Log.i(TAG, "handleMessage(" + message + ")");
        }
        mNegotiator.setTime(System.currentTimeMillis());
        InquiryMessage response = mNegotiator.advanceStateMachine(message);
        if (response != null) {
            Log.i(TAG,"send response: " + response);
            MidiWriter buffer = new MidiWriter();
            int len = response.encode(buffer);
            mTargetReceiver.send(buffer.getData(), 0, len);
        }
    }

    public synchronized void start() {
        if (mRunning) return;
        mNegotiator = new CapabilityNegotiator();
        mNegotiator.setInitiator(mInitiator);
        mNegotiator.setSupportedVersion(Midi.VERSION_2_0);
        mRunning = true;
        mThread = new Thread(this);
        mThread.start();
    }

    @Override
    public void run() {
        while (mRunning && !mNegotiator.isFinished()) {
            try {
                Thread.sleep(PERIOD_MSEC);
                handleMessage(null); // to initiate negotiation, or for timeouts
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (IOException e) {
                e.printStackTrace();
                mRunning = false;
            }
        }
        Log.i(TAG,"exiting loop!");
    }

    public void setInitiator(boolean b) {
        mInitiator = b;
    }

    public synchronized void stop() {
        if (!mRunning) return;
        mRunning = false;
        mThread.interrupt();
        try {
            mThread.join(10 * PERIOD_MSEC);
            mThread = null;
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    public int getNegotiatedVersion() {
        return (mEnabled)
                ? ((mNegotiator != null) ? mNegotiator.getNegotiatedVersion() : Midi.VERSION_1_0)
                : Midi.VERSION_2_0;
    }
}
