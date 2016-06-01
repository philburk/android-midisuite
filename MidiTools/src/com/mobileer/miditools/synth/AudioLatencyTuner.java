/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.mobileer.miditools.synth;

import android.media.AudioAttributes;
import android.media.AudioTrack;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optimize the buffer size for an AudioTrack based on the underrun count.
 * <p/>
 * This feature was added in N. So we check for the methods using reflection.
 */
public class AudioLatencyTuner {
    private static final String TAG = "AudioLatencyTuner";
    private static boolean lowLatencySupported;
    private static final int STATE_PRIMING = 0;
    private static final int STATE_LOWERING = 1;
    private static final int STATE_RAISING = 2;

    // These are found using reflection.
    private static int mFlagLowLatency; // AudioAttributes.FLAG_LOW_LATENCY
    private static Method mSetBufferSizeMethod = null;
    private static Method mGetBufferCapacityMethod = null;
    private static Method mGetUnderrunCountMethod = null;

    private final int mInitialSize;
    private int mState = STATE_PRIMING;
    private AudioTrack mAudioTrack;
    private int mPreviousUnderrunCount;
    private int mFramesPerBlock = 64;

    static {
        reflectAdvancedMethods();
    }

    public AudioLatencyTuner(AudioTrack track) {
        mAudioTrack = track;
        mInitialSize = track.getBufferSizeInFrames();
    }

    /**
     * Use Java reflection to find the methods added in the N release.
     */
    private static void reflectAdvancedMethods() {
        try {
            Field field = AudioAttributes.class.getField("FLAG_LOW_LATENCY");
            mFlagLowLatency = field.getInt(AudioAttributes.class);
            lowLatencySupported = true;
        } catch (NoSuchFieldException e) {
            lowLatencySupported = false;
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }

        Method[] methods = AudioTrack.class.getMethods();

        for (Method method : methods) {
            if (method.getName().equals("setBufferSizeInFrames")) {
                mSetBufferSizeMethod = method;
                break;
            }
        }

        for (Method method : methods) {
            if (method.getName().equals("getBufferCapacityInFrames")) {
                mGetBufferCapacityMethod = method;
                break;
            }
        }

        for (Method method : methods) {
            if (method.getName().equals("getUnderrunCount")) {
                mGetUnderrunCountMethod = method;
                break;
            }
        }
    }

    /**
     * @return number of times the audio buffer underflowed and glitched.
     */
    public int getUnderrunCount() {
        // Call using reflection.
        if (mGetUnderrunCountMethod != null && mAudioTrack != null) {
            try {
                Object result = mGetUnderrunCountMethod.invoke(mAudioTrack);
                int count = ((Integer) result).intValue();
                return count;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return 0;
    }

    /**
     * @return allocated size of the buffer
     */
    public int getBufferCapacityInFrames() {
        if (mGetBufferCapacityMethod != null) {
            try {
                Object result = mGetBufferCapacityMethod.invoke(mAudioTrack);
                int size = ((Integer) result).intValue();
                return size;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return mInitialSize;
    }

    /**
     * Set the amount of the buffer capacity that we want to use.
     * Lower values will reduce latency but may cause glitches.
     * Note that you may not get the size you asked for.
     *
     * @return actual size of the buffer
     */
    public int setBufferSizeInFrames(int thresholdFrames) {
        if (mSetBufferSizeMethod != null) {
            try {
                Object result = mSetBufferSizeMethod.invoke(mAudioTrack, thresholdFrames);
                int actual = ((Integer) result).intValue();
                Log.i(TAG, "setThresholdInFrames(" + thresholdFrames + ") returned " + actual);
                return actual;
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            } catch (InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return mInitialSize;
    }

    public void setFramesPerBlock(int framesPerBlock) {
        mFramesPerBlock = framesPerBlock;
    }

    public int getBufferSizeInFrames() {
        return mAudioTrack.getBufferSizeInFrames();
    }

    public static boolean isLowLatencySupported() {
        return lowLatencySupported;
    }

    public static int getLowLatencyFlag() {
        return mFlagLowLatency;
    }

    public void reset() {
        if (!lowLatencySupported) {
            return;
        }
        mState = STATE_PRIMING;
        setBufferSizeInFrames(mInitialSize);
    }

    /**
     * This should be called after every write().
     * It will lower the latency until there are underruns.
     * Then it raises the latency until the underruns stop.
     */
    public void update() {
        if (!lowLatencySupported) {
            return;
        }
        int nextState = mState;
        int underrunCount;
        switch (mState) {
            case STATE_PRIMING:
                if (mAudioTrack.getPlaybackHeadPosition() > (8 * mFramesPerBlock)) {
                    nextState = STATE_LOWERING;
                    mPreviousUnderrunCount = getUnderrunCount();
                }
                break;
            case STATE_LOWERING:
                underrunCount = getUnderrunCount();
                if (underrunCount > mPreviousUnderrunCount) {
                    nextState = STATE_RAISING;
                } else {
                    if (incrementThreshold(-1)) {
                        // If we hit bottom then start raising it back up.
                        nextState = STATE_RAISING;
                    }
                }
                mPreviousUnderrunCount = underrunCount;
                break;
            case STATE_RAISING:
                underrunCount = getUnderrunCount();
                if (underrunCount > mPreviousUnderrunCount) {
                    incrementThreshold(1);
                }
                mPreviousUnderrunCount = underrunCount;
                break;
        }
        mState = nextState;
    }

    private boolean incrementThreshold(int delta) {
        int original = getBufferSizeInFrames();
        int numBlocks = original / mFramesPerBlock;
        numBlocks += delta;
        int target = numBlocks * mFramesPerBlock;
        int actual = setBufferSizeInFrames(target);
        Log.i(TAG, "Threshold changed from " + original + " to " + actual);
        return actual == original;
    }

}
