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

import android.annotation.TargetApi;
import android.media.AudioAttributes;
import android.media.AudioTrack;
import android.os.Build;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Optimize the buffer size for an AudioTrack based on the underrun count.
 * Just call update() after every write() to the AudioTrack.
 *
 * The buffer size determines the latency.
 * Lower the latency until there are glitches.
 * Then raise the latency until the glitches stop.
 *
 * <p/>
 * This feature was added in N. So we check for support based on the SDK version.
 */
public class AudioLatencyTuner {
    private static final String TAG = "AudioLatencyTuner";
    private static final int STATE_PRIMING = 0;
    private static final int STATE_LOWERING = 1;
    private static final int STATE_RAISING = 2;

    private static boolean mLowLatencySupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;

    private final int mInitialSize;
    private final AudioTrack mAudioTrack;
    private final int mFramesPerBlock;

    private int mState = STATE_PRIMING;
    private int mPreviousUnderrunCount;

    /**
     * An application can determine the optimal framesPerBlock as follows:
     * <pre><code>
     * String text = audioManager.getProperty(AudioManager.PROPERTY_OUTPUT_FRAMES_PER_BUFFER);
     * int framesPerBlock = Integer.parseInt(text);
     * </code></pre>
     * @param track
     * @param framesPerBlock Number of frames processed at one time by the mixer.
     */
    @TargetApi(23)
    public AudioLatencyTuner(AudioTrack track, int framesPerBlock) {
        mAudioTrack = track;
        mInitialSize = track.getBufferSizeInFrames();
        mFramesPerBlock = framesPerBlock;
        reset();
    }

    /**
     * This only works on N or later versions of Android.
     * @return number of times the audio buffer underflowed and glitched.
     */
    @TargetApi(24)
    public int getUnderrunCount() {
        if (mLowLatencySupported) {
            return mAudioTrack.getUnderrunCount();
        } else {
            return 0;
        }
    }

    /**
     * This only works on N or later versions of Android.
     * @return allocated size of the buffer
     */
    @TargetApi(24)
    public int getBufferCapacityInFrames() {
        if (mLowLatencySupported) {
            return mAudioTrack.getBufferCapacityInFrames();
        } else {
            return mInitialSize;
        }
    }

    /**
     * Set the amount of the buffer capacity that we want to use.
     * Lower values will reduce latency but may cause glitches.
     * Note that you may not get the size you asked for.
     *
     * This only works on N or later versions of Android.
     *
     * @return actual size of the buffer
     */
    @TargetApi(24)
    public int setBufferSizeInFrames(int thresholdFrames) {
        if (mLowLatencySupported) {
            return mAudioTrack.setBufferSizeInFrames(thresholdFrames);
        } else {
            return mInitialSize;
        }
    }

    @TargetApi(23)
    public int getBufferSizeInFrames() {
        return mAudioTrack.getBufferSizeInFrames();
    }

    public static boolean isLowLatencySupported() {
        return mLowLatencySupported;
    }

    /**
     * This only works on N or later versions of Android.
     *
     * @return flag used to enable LOW_LATENCY
     */
    @TargetApi(24)
    public static int getLowLatencyFlag() {
        if (mLowLatencySupported) {
            return AudioAttributes.FLAG_LOW_LATENCY;
        } else {
            return 0;
        }
    }

    /**
     * Reset the internal state machine and set the buffer size back to
     * the original size. The tuning process will then restart.
     */
    public void reset() {
        mState = STATE_PRIMING;
        setBufferSizeInFrames(mInitialSize);
    }

    /**
     * This should be called after every write().
     * It will lower the latency until there are underruns.
     * Then it raises the latency until the underruns stop.
     */
    @TargetApi(3)
    public void update() {
        if (!mLowLatencySupported) {
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

    /**
     * Raise or lower the buffer size in blocks.
     * @return true if the size did not change
     */
    private boolean incrementThreshold(int deltaBlocks) {
        int original = getBufferSizeInFrames();
        int numBlocks = original / mFramesPerBlock;
        numBlocks += deltaBlocks;
        int target = numBlocks * mFramesPerBlock;
        int actual = setBufferSizeInFrames(target);
        Log.i(TAG, "Buffer size changed from " + original + " to " + actual);
        return actual == original;
    }

}
