#!/usr/bin/env bash

FROM=74354f979ea8
TO=cad82c3b69bc

FILE=inbound-pushlog-$FROM-$TO.html

echo "This data excludes all merge changesets and the changes that were merged"
echo "Changesets landed: $(cat $FILE | awk '/class="parity[01]  id/ && !/hidden changeset/ { print $0 }' | wc -l)"
echo " Number of pushes: $(cat $FILE | awk '/class="parity[01]  id/ && !/hidden changeset/ && /class="date"/ { print $0 }' | wc -l)"

if [ ! -f pushes.txt ]; then
    cat $FILE | awk -v FROM=$FROM -f inbound-csets.awk > pushes.txt
fi
