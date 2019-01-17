#!/bin/bash

javac -d build/ $(find src/ -name *.java)
cd build
jar cmf ../manifest/client.mf ../jars/client.jar $(find client/ -name *.class) utils/Utils.class
jar cmf ../manifest/server.mf ../jars/server.jar $(find server/ -name *.class) utils/Utils.class
