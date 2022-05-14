#!/usr/bin/env bash

# Reads a line in the format "$host:$key=$value", setting those variables.
function readconf {
  local conf
  IFS=':' read host conf <<< "$1"
  IFS='=' read key value <<< "$conf"
}
# Parse a properties file for given key
# $1 is properties file name
# $2 is the config key to look for
# $3 is the pass back variable which will have the result
# $4 is either "host" or "value" that needs to be passed back
function parse_peer_file {
  local MASTER_IPS
  for line in $(cat "$1")
  do
    readconf "$line"
    case $key in
      $2)
        if [[ -n "$4" ]]; then
          case $4 in
            "value")
              actual_value="$value" ;;
            *)
              actual_value="$host" ;;
          esac
        else
          actual_value="$host"
        fi
        # Append to comma-separated MASTER_IPS.
        if [[ -n "$MASTER_IPS" ]]; then
          MASTER_IPS="${MASTER_IPS},"
        fi
        MASTER_IPS="${MASTER_IPS}${actual_value}"
        ;;
    esac
  done
  # assign the master ips to pass back variable
  eval "$3='$MASTER_IPS'"
}
