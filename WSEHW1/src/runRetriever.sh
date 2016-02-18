#!/bin/bash

if [ "$#" -lt 2 ]
then
   echo "Usage: <indexerDir> <query>"
   exit 1
fi
indexerDir="$1"
shift

java -cp .:../Lucene/*:../HTMLParser/* Retriever "$1" "$@"
