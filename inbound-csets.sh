#!/usr/bin/env bash

FROM=74354f979ea8
TO=cad82c3b69bc
REPO=/Users/kats/zspace/mozilla

FILE=inbound-pushlog-$FROM-$TO.html

if [ ! -f $FILE ]; then
    curl "https://hg.mozilla.org/integration/mozilla-inbound/pushloghtml?fromchange=$FROM&tochange=$TO" > $FILE
fi

echo "This data excludes all merge changesets and the changes that were merged"
echo "Changesets landed: $(cat $FILE | awk '/class="parity[01]  id/ && !/hidden changeset/ { print $0 }' | wc -l)"
echo " Number of pushes: $(cat $FILE | awk '/class="parity[01]  id/ && !/hidden changeset/ && /class="date"/ { print $0 }' | wc -l)"

if [ ! -f pushes-nobackouts.txt ]; then
    cat $FILE | awk -v FROM=$FROM -f inbound-csets.awk > pushes.txt

    rm pushes-details.txt
    cat pushes.txt |
    while read push next; do
        hg -R $REPO log -r "reverse(children($next)..$push)" --template "{node|short} {desc|firstline}\n" >> pushes-details.txt
        echo "" >> pushes-details.txt
    done
    echo "Please copy pushes-details.txt to pushes-nobackouts.txt and remove all backout changesets and "
    echo "the things they backed out. Then re-run this script to continue."
    exit 0
fi

if [ ! -f conflicts.txt ]; then
    hg -R $REPO update $TO
    find $REPO -name "*.rej" | xargs rm -f
    cat pushes-nobackouts.txt |
    awk 'BEGIN { first=0; last=0 } !/^$/ { if (first == 0) { first=$1 } last=$1 } /^$/ { print first, last; first=0 }' |
    while read BACKOUT_FROM BACKOUT_TO; do
        echo "Testing push $BACKOUT_FROM..."
        BACKOUT_TO=$(hg -R $REPO parents -r $BACKOUT_TO --template '{node|short}\n' | head -n 1)
        hg -R $REPO diff -r $BACKOUT_FROM -r $BACKOUT_TO | hg -R $REPO qimport - -n try_backout >/dev/null 2>&1
        hg -R $REPO qpush >/dev/null 2>&1
        CONFLICT=$?
        hg -R $REPO qpop >/dev/null 2>&1
        hg -R $REPO qrm try_backout >/dev/null 2>&1
        if [ $CONFLICT -ne 0 ]; then
            echo "Conflicts for push $BACKOUT_FROM" >> conflicts.txt
            for REJECT in $(find $REPO -name "*.rej"); do
                FILE=${REJECT%.rej}
                echo -n "    ${FILE#$REPO/}" >> conflicts.txt
                hg -R $REPO log -r "children($BACKOUT_FROM)..$TO" --template " {node|short}" $FILE >> conflicts.txt
                echo "" >> conflicts.txt
                rm $REJECT
            done
        fi
    done
fi
