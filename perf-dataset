#!/bin/bash

export POINTFILE=hdfs://ec2-184-72-87-26.compute-1.amazonaws.com:9000/1e7.dat
MESOSMASTER=1@10.202.70.163:5050
DATE=$(date +%Y%m%dT%H%M%S)
MANIPULATIONS=5
TRIALS=3
LOGDIR=run-$DATE

mkdir $LOGDIR

for manip in $(seq 0 $(expr $MANIPULATIONS - 1))
do
    export manip
    POINTFILEREPEATED=$(perl -e 'print join(",", ($ENV{"POINTFILE"}) x (2 ** $ENV{"manip"}))')
    for trial in $(seq 1 $TRIALS)
    do
	./run SparkKMeans $POINTFILEREPEATED 3 $MESOSMASTER 7,40 45,35 20,8 2>&1 | tee $LOGDIR/$manip-$trial.log
    done
done
