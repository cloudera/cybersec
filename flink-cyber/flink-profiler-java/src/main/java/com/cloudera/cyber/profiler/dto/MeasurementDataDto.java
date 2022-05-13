package com.cloudera.cyber.profiler.dto;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@AllArgsConstructor
@Builder
public class MeasurementDataDto implements Serializable {
    private Integer measurementId;
    private Integer profileId;
    private ArrayList<String> keys;
    private String measurementName;
    private String measurementType;
    private Timestamp measurementTime;
    private Double measurementValue;
}
