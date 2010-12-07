#!/bin/bash

mkdir -p $LOGDIR

source $AWSKEYFILE

echo "Launching cluster..."
mesos-ec2 \
    --slaves=$NUMSLAVES \
    --key-pair=$KEYPAIR \
    --identity-file=$KEYPAIRFILE \
    --instance-type=$INSTANCETYPE \
    --zone=$ZONE launch $CLUSTERNAME 2>&1 | sed 's/^/    /'

MASTER=$(mesos-ec2 -k $KEYPAIR -i $KEYPAIRFILE get-master $CLUSTERNAME | tail -n 1)
MESOSMASTER=$(curl http://$MASTER:8080 2>/dev/null | perl -ne 'print $1 if /^PID: (.*)<br \/>/')
echo "Master public hostname: $MASTER"
echo "Master Mesos PID: $MESOSMASTER"

echo "Configuring cluster: Updating Spark from Git..."
ssh -i $KEYPAIRFILE root@$MASTER \
    "cd /root/spark; \
      git pull git://github.com/mesos/spark.git master; \
      make clean" 2>&1 | sed 's/^/    /'
echo \
    "export MESOS_HOME=/root/mesos
      export SCALA_HOME=/root/scala-2.8.0.final
      export SPARK_CLASSPATH=/root/kmeans-spark/build/kmeans-spark.jar
      export SPARK_MEM=$MEMORY
      export SPARK_JAVA_OPTS=\"-Dspark.cache.class=spark.BoundedMemoryCache\"" > /tmp/$CLUSTERNAME-spark-env.sh
scp -i $KEYPAIRFILE /tmp/$CLUSTERNAME-spark-env.sh root@$MASTER:/root/spark/conf/spark-env.sh 2>&1 | sed 's/^/    /'

echo "Configuring cluster: Setting up kmeans-spark..."
ssh -i $KEYPAIRFILE root@$MASTER \
    "apt-get -y install mercurial; \
      cd /root; \
      hg clone http://ankurdave.com/hg/kmeans-spark; \
      cd kmeans-spark; \
      source ~/.profile; \
      make" 2>&1 | sed 's/^/    /'

echo "Configuring cluster: Copying modified files to slaves..."
ssh -i $KEYPAIRFILE root@$MASTER \
    "cd /root; \
      mesos-ec2/copy-dir spark; \
      mesos-ec2/copy-dir kmeans-spark" 2>&1 | sed 's/^/    /'

echo "Generating point file..."
ssh -i $KEYPAIRFILE root@$MASTER \
    "source ~/.profile; \
      cd kmeans-spark; \
      ./run KMeansDataGenerator 3 $NUMPOINTS 1 > pts.dat; \
      ../hadoop-0.20.2/bin/hadoop fs -put pts.dat /" 2>&1 | sed 's/^/    /'
export POINTFILE=hdfs://$MASTER:9000/pts.dat
