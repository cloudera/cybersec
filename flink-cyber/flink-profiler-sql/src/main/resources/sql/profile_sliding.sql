SELECT
    UUID() as id,
    HOP_START(ts, interval '${intervalValue}' ${intervalUnits}) as startTs,
    HOP_END(ts, interval '${intervalValue}' ${intervalUnits}) as endTs,
    '${profileName}' as profile,
    `${profileEntity}` as entity,
    ROW(${profileFields}) as profile
FROM profiler.sources.`${profileSource}`
${profileFilter}
GROUP BY
HOP(ts, interval '${intervalValue}' ${intervalUnits}),
`${profileEntity}`
