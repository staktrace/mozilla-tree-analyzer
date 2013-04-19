#!/usr/bin/env bash

cat inbound-treestatus | awk -f inbound-uptime.awk
