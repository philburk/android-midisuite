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

package com.android.miditools;

import static org.junit.Assert.*;

import org.junit.Test;


// Uncomment this import if you want to test the internal MidiFramer.
// import com.android.internal.midi.MidiFramer;

/**
 * Unit Tests for the EventScheduler
 */
public class TestEventScheduler {


    @Test
    public void testEventPool() {
        EventScheduler scheduler = new EventScheduler();
        long time1 = 723L;
        EventScheduler.SchedulableEvent event1 = new EventScheduler.SchedulableEvent(time1);
        assertEquals("event time", time1, event1.getTimestamp());
        assertEquals("empty event pool", null,  scheduler.removeEventfromPool());
        scheduler.addEventToPool(event1);
        assertEquals("always leave one event in pool", null,  scheduler.removeEventfromPool());

        long time2 = 9817L;
        EventScheduler.SchedulableEvent event2 = new EventScheduler.SchedulableEvent(time2);
        scheduler.addEventToPool(event2);
        assertEquals("first event in pool", event1,  scheduler.removeEventfromPool());
        assertEquals("always leave one event in pool", null,  scheduler.removeEventfromPool());
        scheduler.addEventToPool(event1);
        assertEquals("second event in pool", event2,  scheduler.removeEventfromPool());
    }

    @Test
    public void testSingleEvent() {
        EventScheduler scheduler = new EventScheduler();
        long time = 723L;
        EventScheduler.SchedulableEvent event = new EventScheduler.SchedulableEvent(time);
        assertEquals("event time", time, event.getTimestamp());
        scheduler.add(event);
        assertEquals("too soon", null, scheduler.getNextEvent(time - 1));
        assertEquals("right now", event, scheduler.getNextEvent(time));
    }

    @Test
    public void testTwoEvents() {
        EventScheduler scheduler = new EventScheduler();
        long time1 = 723L;
        EventScheduler.SchedulableEvent event1 = new EventScheduler.SchedulableEvent(time1);
        long time2 = 9817L;
        EventScheduler.SchedulableEvent event2 = new EventScheduler.SchedulableEvent(time2);
        scheduler.add(event1);
        scheduler.add(event2);
        assertEquals("too soon", null, scheduler.getNextEvent(time1 - 1));
        assertEquals("after 1", event1, scheduler.getNextEvent(time1 + 5));
        assertEquals("too soon", null, scheduler.getNextEvent(time1 + 5));
        assertEquals("after 2", event2, scheduler.getNextEvent(time2 + 7));
    }

    @Test
    public void testReverseTwoEvents() {
        EventScheduler scheduler = new EventScheduler();
        long time1 = 723L;
        EventScheduler.SchedulableEvent event1 = new EventScheduler.SchedulableEvent(time1);
        long time2 = 9817L;
        EventScheduler.SchedulableEvent event2 = new EventScheduler.SchedulableEvent(time2);
        scheduler.add(event2);
        scheduler.add(event1);
        assertEquals("too soon", null, scheduler.getNextEvent(time1 - 1));
        assertEquals("after 1", event1, scheduler.getNextEvent(time1 + 5));
        assertEquals("too soon", null, scheduler.getNextEvent(time1 + 5));
        assertEquals("after 2", event2, scheduler.getNextEvent(time2 + 7));
    }
}
