/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.flink;

import lombok.extern.java.Log;
import org.apache.flink.api.common.functions.ReduceFunction;
import org.apache.flink.api.common.state.ReducingState;
import org.apache.flink.api.common.state.ReducingStateDescriptor;
import org.apache.flink.api.common.typeutils.base.LongSerializer;
import org.apache.flink.streaming.api.windowing.triggers.Trigger;
import org.apache.flink.streaming.api.windowing.triggers.TriggerResult;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;

@Log
public class EventTimeAndCountTrigger extends Trigger<Object, TimeWindow> {

    private final long maxCount;
    private final ReducingStateDescriptor<Long> stateDesc;

    private EventTimeAndCountTrigger(long maxCount) {
        this.stateDesc =
              new ReducingStateDescriptor("count", new EventTimeAndCountTrigger.Sum(), LongSerializer.INSTANCE);
        this.maxCount = maxCount;
    }

    public TriggerResult onElement(Object element, long timestamp, TimeWindow window, TriggerContext ctx)
          throws Exception {
        ReducingState<Long> count = (ReducingState) ctx.getPartitionedState(this.stateDesc);
        count.add(1L);
        log.finest(
              String.format("onElement: %s count: %d, timestamp %s, maxTime: %s, watermark: %d", element, count.get(),
                    timestamp, window.maxTimestamp(), ctx.getCurrentWatermark()));
        if ((Long) count.get() >= this.maxCount || window.maxTimestamp() <= ctx.getCurrentWatermark()) {
            count.clear();
            //ctx.registerEventTimeTimer(window.maxTimestamp());
            return TriggerResult.FIRE_AND_PURGE;
        } else {
            ctx.registerEventTimeTimer(window.maxTimestamp());
            return TriggerResult.CONTINUE;
        }
    }

    @SuppressWarnings("checkstyle:EmptyCatchBlock")
    public TriggerResult onEventTime(long timestamp, TimeWindow window, TriggerContext ctx) {
        try {
            log.finest(String.format("onEventTime: count: %d, timestamp %d, maxTime: %d, watermark: %d",
                  ctx.getPartitionedState(this.stateDesc).get(), timestamp, window.maxTimestamp(),
                  ctx.getCurrentWatermark()));
        } catch (Exception ignored) {
        }

        if (timestamp == window.maxTimestamp()) {
            ReducingState<Long> count = (ReducingState) ctx.getPartitionedState(this.stateDesc);
            count.clear();
            return TriggerResult.FIRE_AND_PURGE;
        } else {
            return TriggerResult.CONTINUE;
        }
    }

    public TriggerResult onProcessingTime(long time, TimeWindow window, TriggerContext ctx) throws Exception {
        return TriggerResult.CONTINUE;
    }

    public void clear(TimeWindow window, TriggerContext ctx) throws Exception {
        ctx.deleteEventTimeTimer(window.maxTimestamp());
        ((ReducingState) ctx.getPartitionedState(this.stateDesc)).clear();
    }

    public boolean canMerge() {
        return true;
    }

    public void onMerge(TimeWindow window, OnMergeContext ctx) throws Exception {
        ctx.mergePartitionedState(this.stateDesc);
        long windowMaxTimestamp = window.maxTimestamp();
        if (windowMaxTimestamp > ctx.getCurrentWatermark()) {
            ctx.registerEventTimeTimer(windowMaxTimestamp);
        }
    }

    public String toString() {
        return "EventTimeAndCountTrigger(" + this.maxCount + ")";
    }

    public static EventTimeAndCountTrigger of(long maxCount) {
        return new EventTimeAndCountTrigger(maxCount);
    }

    private static class Sum implements ReduceFunction<Long> {
        private static final long serialVersionUID = 1L;

        private Sum() {
        }

        public Long reduce(Long value1, Long value2) throws Exception {
            return value1 + value2;
        }
    }


}
