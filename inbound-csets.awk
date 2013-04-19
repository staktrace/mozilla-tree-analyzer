function cset(x) {
    return substr(x, match(x, "rev/") + 4, 12)
}

BEGIN {
    prevlast = 0
    hidden = 0
}

/class="parity[01]  id/ && /class="date"/ {
    if (prevlast != 0 && !hidden) {
        print prevlast, cset($0)
    }
    hidden = match($0, "hidden changeset")
    prevlast = cset($0)
}

END {
    print prevlast, FROM
}
