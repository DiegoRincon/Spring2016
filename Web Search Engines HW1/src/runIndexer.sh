#!/bin/bash

if [ ! -d "indexer" ];
then
   echo "running indexer on data folder $1"
   java -cp .:../../lib/* Indexer indexer/ "$1";
fi

shift

java -cp .:../../lib/* Retriever indexer/ $@;
