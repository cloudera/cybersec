SELECT
    UUID() as id,
    tumble_start(ts, interval '${intervalValue}' ${intervalUnits}) as startTs,
    tumble_end(ts, interval '${intervalValue}' ${intervalUnits}) as endTs,
    '${profileName}' as profile,
    `${profileEntity}` as entity,
    ROW(${profileFields}) as profile
FROM profiler.sources.`${profileSource}`
${profileFilter}
GROUP BY
tumble(ts, interval '${intervalValue}' ${intervalUnits}),
`${profileEntity}`
