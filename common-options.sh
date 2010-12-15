#!/bin/bash

DATE=$(date +%Y%m%dT%H%M%S)
TRIALS=3
KEYPAIR=Mesos
KEYPAIRFILE=/work/ankurd/keys/Mesos.pem
AWSKEYFILE=/work/ankurd/keys/env.sh
ZONE=us-east-1d
CLUSTERNAME=${PERF_CLUSTERNAME:-KMeans}
