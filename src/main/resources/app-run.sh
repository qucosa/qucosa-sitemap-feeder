#!/bin/bash

if [ ! -z "$KAFKA_BROKER" ]
then
  echo "kafka.broker.host=$KAFKA_BROKER" > docker.properties
fi

if [ ! -z "$SERVER_PORT" ]
then
  echo "server.port=$SERVER_PORT" >> docker.properties
fi

if [ ! -z "$SITEMAP_SERVICE" ]
then
  echo "sitemap.service.url=$SITEMAP_SERVICE" >> docker.properties
fi

if [ ! -z "$CONFIG_PATH" ]
then
  echo "conf.path=$CONFIG_PATH" >> docker.properties
fi

if [ ! -z "$FEDORA_SERVICE" ]
then
  echo "fedora.service.url=$FEDORA_SERVICE" >> docker.properties
fi

java -jar qucosa-sitemap-feeder.jar "./docker.properties" $@