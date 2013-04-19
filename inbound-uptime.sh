#!/usr/bin/env bash

echo "Total downtime calculation"
cat inbound-treestatus | awk -f inbound-uptime.awk -v 'filter= '
echo ""
echo "Bustage downtime calculation"
cat inbound-treestatus | awk -f inbound-uptime.awk -v 'filter=Bustage'
echo ""
echo "Infra downtime calculation"
cat inbound-treestatus | awk -f inbound-uptime.awk -v 'filter=infra'
echo ""
