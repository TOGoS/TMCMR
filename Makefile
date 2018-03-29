.PHONY: all clean

all: TMCMR.jar.urn

target_dir := target/main

clean:
	rm -rf target TMCMR.jar .src.lst

TMCMR.jar: $(shell find src) Makefile
	rm -rf ${target_dir} TMCMR.jar
	mkdir -p ${target_dir}
	rsync -rt src/main/java/ ${target_dir}/
	find src/main/java -name '*.java' >.src.lst
	javac -source 1.6 -target 1.6 -d ${target_dir} @.src.lst
	mkdir -p ${target_dir}/META-INF
	echo 'Version: 1.0' >${target_dir}/META-INF/MANIFEST.MF
	echo 'Main-Class: togos.minecraft.maprend.RegionRenderer' >>${target_dir}/META-INF/MANIFEST.MF
	cd ${target_dir} ; zip -9 -r ../../TMCMR.jar . ; cd ..

%.urn: % TMCMR.jar
	java -cp TMCMR.jar togos.minecraft.maprend.io.IDFile "$<" >"$@"
