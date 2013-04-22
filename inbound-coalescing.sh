#!/usr/bin/env bash

mkdir raw-build-data
pushd raw-build-data
for ((i = 6; i <= 9; i++)); do
    if [ ! -f "builds-2013-04-0$i.js" ]; then
        wget "http://builddata.pub.build.mozilla.org/buildjson/builds-2013-04-0$i.js.gz" && gunzip "builds-2013-04-0$i.js.gz"
    fi
done
for ((i = 10; i <= 17; i++)); do
    if [ ! -f "builds-2013-04-$i.js" ]; then
        wget "http://builddata.pub.build.mozilla.org/buildjson/builds-2013-04-$i.js.gz" && gunzip "builds-2013-04-$i.js.gz"
    fi
done
popd

javac -cp sts_util.jar BuildTimes.java CoalesceStats.java
if [ ! -f "build-time-table.csv" ]; then
    java -Xmx2048M -cp sts_util.jar:. BuildTimes pushes.txt raw-build-data > build-time-table.csv
fi
java -cp sts_util.jar:. CoalesceStats build-time-table.csv pushes-nobackouts.txt
