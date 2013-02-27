#!/bin/sh

rm -rf bin TMCMR.jar
cp -r src bin
find src -name *.java >.src.lst
javac -d bin @.src.lst
cd bin
mkdir META-INF
echo 'Version: 1.0' >META-INF/MANIFEST.MF
echo 'Main-Class: togos.minecraft.maprend.RegionRenderer' >>META-INF/MANIFEST.MF
zip -r ../TMCMR.jar .
