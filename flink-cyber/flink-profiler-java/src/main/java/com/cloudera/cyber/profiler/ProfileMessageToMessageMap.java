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

package com.cloudera.cyber.profiler;

import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_SOURCE;
import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_TOPIC_NAME;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import org.apache.flink.api.common.functions.MapFunction;

public class ProfileMessageToMessageMap implements MapFunction<ProfileMessage, Message> {

    @Override
    public Message map(ProfileMessage profileMessage) throws Exception {

        SignedSourceKey sourceKey = SignedSourceKey.builder()
                                                   .topic(PROFILE_TOPIC_NAME)
                                                   .partition(0)
                                                   .offset(0)
                                                   .signature(new byte[1])
                                                   .build();
        return Message.builder()
                      .extensions(profileMessage.getExtensions())
                      .ts(profileMessage.getTs())
                      .originalSource(sourceKey)
                      .source(PROFILE_SOURCE)
                      .build();
    }
}
