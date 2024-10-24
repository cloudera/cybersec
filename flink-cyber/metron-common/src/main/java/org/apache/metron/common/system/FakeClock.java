/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.common.system;

import java.util.concurrent.TimeUnit;

/**
 * A fake clock for test purposes, that starts out at time zero (epoch), and
 * never advances itself, but allows you to increment it by any desired amount.
 *
 * <p>Note that the base class is not the Java 8 Clock, but rather the Clock we
 * defined in {@link org.apache.metron.common.system.Clock}.  Fundamental units of time
 * are milliseconds.
 *
 * <p>Three exceptions are also defined: {@link IllegalArgumentClockNegative},
 * {@link IllegalArgumentClockZero}, and {@link IllegalArgumentClockOverflow}.
 * These are thrown in various circumstances that imply the FakeClock is
 * being used outside of its design intent. They are subclasses of IllegalArgumentException,
 * hence unchecked.
 */
public class FakeClock extends Clock {
    private long nowMs = 0;

    @Override
    public long currentTimeMillis() {
        return nowMs;
    }

    /**
     * Advance the fake clock by a number of milliseconds.
     *
     * @param durationMs The duration of the adjustment
     * @throws IllegalArgumentClockNegative (unchecked) if you try to go backwards in time.
     *     This is not an allowed behavior, because most system clocks go to great
     *     effort to make sure it never happens, even with, e.g., anomalous events
     *     from a bad NTP server.
     *     If we really get a demand for this capability, we'll add methods that don't
     *     check for this.
     * @throws IllegalArgumentClockOverflow (unchecked) if you try to add a duration
     *     that would overflow the Long value of {@code currentTimeMillis}
     */
    public void elapseMillis(long durationMs) {
        long instantMs = nowMs + durationMs;
        if (durationMs < 0) {
            throw new IllegalArgumentClockNegative(String.format(
                  "Attempted to move backward in time, by %d milliseconds.",
                  durationMs));
        } else if (instantMs < 0) {
            throw new IllegalArgumentClockOverflow(String.format(
                  "Attempted to advance beyond the edge of time, to epoch %d + %d.",
                  nowMs, durationMs));
        }
        nowMs = instantMs;
    }

    /**
     * Advance the fake clock by a number of seconds.
     * See {@code elapseMillis} for details.
     *
     * @param durationSecs The duration to elapse in seconds
     */
    public void elapseSeconds(long durationSecs) {
        elapseMillis(TimeUnit.SECONDS.toMillis(durationSecs));
    }

    /**
     * Advance the fake clock to a point in time specified as milliseconds after 0.
     *
     * @param instantMs - epoch time in milliseconds
     * @throws IllegalArgumentClockNegative (unchecked) if you try to go backwards in time.
     *     This is not an allowed behavior, because most system clocks go to great
     *     effort to make sure it never happens, even with, e.g., anomalous events
     *     from a bad NTP server.
     *     If we really get a demand for this capability, we'll add methods that don't
     *     check for this.
     * @throws IllegalArgumentClockZero (unchecked) if you try to "advance" the clock to the time it already is.
     *     Why?  Because it implies your test code has lost track of previous increments,
     *     which might be problematic, so we do this in the spirit of "fail fast".
     *     If you *meant* to lose track, for instance if you were using random numbers of events,
     *     or whatever, you can always orient yourself in time by reading {@code currentTimeMillis}.
     */
    public void advanceToMillis(long instantMs) {
        if (instantMs < nowMs) {
            throw new IllegalArgumentClockNegative(String.format(
                  "Attempted to move backward in time, from epoch %d to %d.",
                  nowMs, instantMs));
        }
        if (instantMs == nowMs) {
            throw new IllegalArgumentClockZero(String.format(
                  "Time was set to current time, with null advance, at epoch %d.",
                  nowMs));
        }
        nowMs = instantMs;
    }

    /**
     * Advance the fake clock to a point in time specified as seconds after 0.
     * See {@code advanceToMillis} for details.
     *
     * @param instantSecs - epoch time in seconds
     */
    public void advanceToSeconds(long instantSecs) {
        advanceToMillis(TimeUnit.SECONDS.toMillis(instantSecs));
    }

    /**
     * IllegalArgumentClockNegative (unchecked) is thrown if you try to go backwards in time.
     * This is not an allowed behavior, because most system clocks go to great
     * effort to make sure it never happens, even with, e.g., anomalous events
     * from a bad NTP server.
     * If we really get a demand for this capability, we'll add methods that don't
     * check for this.
     */
    public static class IllegalArgumentClockNegative extends IllegalArgumentException {
        public IllegalArgumentClockNegative(String s) {
            super(s);
        }
    }

    /**
     * IllegalArgumentClockZero (unchecked) is thrown if you try to "advance" the clock to the time it already is.
     * Why?  Because it implies your test code has lost track of previous increments,
     * which might be problematic, so we do this in the spirit of "fail fast".
     * If you *meant* to lose track, for instance if you were using random numbers of events,
     * or whatever, you can always orient yourself in time by reading {@code currentTimeMillis}.
     *
     * <p>Note that argument does not apply to ellapseMillis(0), so it does not throw
     * this exception.
     */
    public static class IllegalArgumentClockZero extends IllegalArgumentException {
        public IllegalArgumentClockZero(String s) {
            super(s);
        }
    }

    /**
     * IllegalArgumentClockOverflow (unchecked) is thrown if you try to add a duration
     * that would overflow the Long value of {@code currentTimeMillis}.
     */
    public static class IllegalArgumentClockOverflow extends IllegalArgumentException {
        public IllegalArgumentClockOverflow(String s) {
            super(s);
        }
    }

}
