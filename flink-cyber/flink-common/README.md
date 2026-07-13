# Flink Job Common Configuration

# Checkpointing
Add these configurations to the properties file of the jobs to override
the [Flink checkpoint configuration](https://nightlies.apache.org/flink/flink-docs-release-1.20/docs/dev/datastream/fault-tolerance/checkpointing/) for the job.  

| Property Name                        | Type | Flink Setting                                   | Description                                                                                         | Required/Default | Example |
|--------------------------------------|------|-------------------------------------------------|-----------------------------------------------------------------------------------------------------|------------------|---------|
| checkpoint.interval.ms               | long | execution.checkpointing.interval                | Milliseconds between checkpoints during normal operations.                                          | Default = 60000  | 300000  |
| checkpoint.minimum.pause.interval.ms | long | execution.checkpointing.min-pause               | Milliseconds to pause between checkpointing attempts.  Set the amount of time without checkpointing | Default = 60000  | 10000   |                                     
| checkpoint.backlog.interval.ms       | long | execution.checkpointing.interval-during-backlog | Milliseconds between checkpoints when processing backlog.                                           | Default = 300000 | 10000   |
| checkpoint.timeout.ms                | long | execution.checkpointing.timeout                 | Milliseconds for a checkpoint to complete before being discarded.                                   | Default = 600000 | 30000   |
