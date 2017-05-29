#!/bin/bash
# I wrote this because Maven (and/or) Nexus does not behave well when upload
# is slow, so if you try to do the "mvn deploy" from the parent directory of
# the libraries you might end-up with module's metadata that point to
# inexisting artifact jar such as "010203-5" and available jar is one second
# away from perfection "010202-5.jar" which sucks hard
find . -maxdepth 1 -type d -regextype sed -regex ".*/[^.].*" | while read d; do
  (cd $d && mvn deploy)
done
