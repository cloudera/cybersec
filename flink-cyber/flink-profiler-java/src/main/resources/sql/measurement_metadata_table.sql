CREATE TABLE IF NOT EXISTS ${measurement_metadata_table_name} (
    ID BIGINT PRIMARY KEY,
    PROFILE_ID BIGINT,
    FIELD_NAME VARCHAR,
    RESULT_EXTENSION_NAME VARCHAR,
    AGGREGATION_METHOD VARCHAR,
    CALCULATE_STATS BOOLEAN,
    FORMAT VARCHAR,
    FIRST_SEEN_EXPIRATION_DURATION BIGINT,
    FIRST_SEEN_EXPIRATION_DURATION_UNIT VARCHAR
)