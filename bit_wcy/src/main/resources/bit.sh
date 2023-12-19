#!/bin/bash
if [ $# -eq 0 ]; then
    java -jar bit_wcy-1.13-1.6-SNAPSHOT.jar 2>log
fi
if [ "$1" = "--gui" ]; then
    java -jar bit_wcy-1.13-1.6-SNAPSHOT.jar --gui 2>log
fi