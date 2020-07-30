#!/usr/bin/python
import ConfigParser
import os

# Long running python poller, discovers poll able taxii feeds, and sends any stixx to a kafka topic


def read_config(cfg_files):
    if(cfg_files != None):
        config = ConfigParser.RawConfigParser()

        # merges all files into a single config
        for i, cfg_file in enumerate(cfg_files):
            if(os.path.exists(cfg_file)):
                config.read(cfg_file)

        return config

config = read_config('./taxii.properties')

print(config)
# client = create_client(
#     'test.taxiistand.com',
#     use_https=True,
#     discovery_path='/read-write/services/discovery')