INSERT INTO profiler.profiles
SELECT
    UUID() as id,
    tumble_start(ts, interval '${intervalValue}' ${intervalUnits}) as startTs,
    tumble_end(ts, interval '5' minutes) as endTs,
    '${profileName}' as profile,
    `${profileEntity}` as entity,
    ROW(${profileFields}) as profile
FROM profiler.sources.`${profileSource}`
GROUP BY
tumble(ts, interval '${intervalValue}' ${intervalUnits}),
`${profileEntity}`
