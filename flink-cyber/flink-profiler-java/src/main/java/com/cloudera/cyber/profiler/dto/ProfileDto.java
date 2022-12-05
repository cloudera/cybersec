package com.cloudera.cyber.profiler.dto;

import com.cloudera.cyber.profiler.ProfileGroupConfig;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@AllArgsConstructor
@ToString
@Builder(toBuilder = true)
public class ProfileDto implements Serializable {
    private Integer id;
    private String profileGroupName;
    private String keyFieldNames;
    private Long periodDuration;
    private String periodDurationUnit;
    private Long statsSlide;
    private String statsSlideUnit;
    private ArrayList<MeasurementDto> measurementDtos;

    public static ProfileDto of(ProfileGroupConfig profileGroup) {
        ArrayList<String> keyFieldNames = new ArrayList<>(profileGroup.getKeyFieldNames());
        return ProfileDto.builder()
                .profileGroupName(profileGroup.getProfileGroupName())
                .keyFieldNames(keyFieldNames.stream().sorted().collect(Collectors.joining(",")))
                .measurementDtos(new ArrayList<>(profileGroup.getMeasurements().stream().map(MeasurementDto::of).collect(Collectors.toList())))
                .periodDuration(profileGroup.getPeriodDuration())
                .periodDurationUnit(profileGroup.getPeriodDurationUnit())
                .statsSlide(profileGroup.getStatsSlide())
                .statsSlideUnit(profileGroup.getStatsSlideUnit())
                .build();
    }
}
