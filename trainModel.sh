#!/bin/bash

if [ ! -e target/scala-2.11/roma.jar ]
then
    echo "You have to execute 'sbt assembly' before submitting your app"
    exit -1
fi

which spark-submit
if [ $? -eq 0 ]
then
    mkdir -p /tmp/data/
    cp -R src/main/resources* /tmp/data/
    spark-submit \
      --driver-memory 6g \
      --class com.github.pedrovgs.roma.machinelearning.RomaMachineLearningTrainer \
      --deploy-mode client \
      target/scala-2.11/roma.jar
else
    echo "Review your Apache Spark installation. We can't find 'spark-submit' binary file."
fi
