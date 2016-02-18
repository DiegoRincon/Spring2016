#!/bin/bash

if [ "$#" -ne 2 ]
then
   echo "Usage: <indexerDir> <datDir>"
   exit 1
fi

java -cp .:../Lucene/*:../HTMLParser/* Indexer "$1" "$2"
