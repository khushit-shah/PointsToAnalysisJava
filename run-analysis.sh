#!/usr/bin/env bash

set -e

## ./run-analysis-one.sh  <Dir>  <MainClass>  <TargetClass>  <TargetMethod>


#./run-analysis-one.sh "./target1-pub" "AddNumbers"  "AddNumbers"  "main"
#./run-analysis-one.sh "./target1-pub" "AddNumFun"   "AddNumFun"   "expr"


# XXX you can add / delete / comment / uncomment lines below
./run-analysis-one.sh "./target1-pub" "BasicTest"   "BasicTest"   "fun1"
./run-analysis-one.sh "./target1-pub" "BasicTest"   "BasicTest"   "fun2"
./run-analysis-one.sh "./target1-pub" "BasicTest"   "BasicTest"   "fun3"
./run-analysis-one.sh "./target1-pub" "BasicTest"   "BasicTest"   "fun4"
./run-analysis-one.sh "./target1-pub" "BasicTest"   "BasicTest"   "fun5"
./run-analysis-one.sh "./target1-pub" "BasicTest"   "BasicTest"   "fun6"
./run-analysis-one.sh "./target2-mine" "MyTest"   "MyTest"   "assign_check"

