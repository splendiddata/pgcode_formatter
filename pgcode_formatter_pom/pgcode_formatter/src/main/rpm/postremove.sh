#!/bin/bash
rm -rf /usr/local/splendiddata/pgcode_formatter
if [ ! "$(ls -A /usr/local/splendiddata)" ] 
then
    rmdir /usr/local/splendiddata
fi
