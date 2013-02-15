#!/bin/sh

rm -rf bin
cp -r src bin
find src -name *.java >.src.lst
javac -d bin @.src.lst
