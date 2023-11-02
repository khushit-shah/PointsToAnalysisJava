#!/usr/bin/env bash

set -e

# export CLASSPATH=.:pkgs/soot-4.3.0-20210915.120431-213-jar-with-dependencies.jar
# export CLASSPATH=.:pkgs/soot-4.3.0-with-deps.jar
source environ.sh


echo === building javac -g LatticeElement.java Analysis.java LatticeElement.java PointsToLatticeElement.java Helper.java ProgramPoint.java Kildalls.java
javac -g LatticeElement.java Analysis.java LatticeElement.java PointsToLatticeElement.java Helper.java ProgramPoint.java Kildalls.java




