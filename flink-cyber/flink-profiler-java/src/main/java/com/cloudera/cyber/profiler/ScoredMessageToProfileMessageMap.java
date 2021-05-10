package com.cloudera.cyber.profiler;

import com.cloudera.cyber.scoring.ScoredMessage;
import org.apache.flink.api.common.functions.MapFunction;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ScoredMessageToProfileMessageMap implements MapFunction<ScoredMessage, ProfileMessage> {

    public static final String CYBER_SCORE_FIELD = "cyberscore";

    private final List<String> allFieldNames;

    public ScoredMessageToProfileMessageMap(ProfileGroupConfig profileGroupConfig) {
        this.allFieldNames = profileGroupConfig.getMeasurementFieldNames();
        this.allFieldNames.addAll(profileGroupConfig.getKeyFieldNames());
    }

    @Override
    public ProfileMessage map(ScoredMessage scoredMessage) {
        Map<String, String> reducedExtensions = new HashMap<>();
        Map<String, String> allExtensions = scoredMessage.getMessage().getExtensions();
        allFieldNames.forEach(fieldName -> {{
            if (CYBER_SCORE_FIELD.equals(fieldName)) {
                reducedExtensions.put(CYBER_SCORE_FIELD, scoredMessage.getCyberScore().toString());
            } else {
                String extensionValue = allExtensions.get(fieldName);
                if (extensionValue != null) {
                    reducedExtensions.put(fieldName, extensionValue);
                }
            }
        }});

       return new ProfileMessage(scoredMessage.getTs(), reducedExtensions);
    }
}
