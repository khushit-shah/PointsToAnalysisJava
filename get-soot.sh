#!/usr/bin/env bash

set -e

mkdir -p pkgs/
curl -L https://oss.sonatype.org/content/repositories/snapshots/org/soot-oss/soot/4.3.0-SNAPSHOT/soot-4.3.0-20211111.165752-236-jar-with-dependencies.jar -o pkgs/soot-4.3.0-with-deps.jar

