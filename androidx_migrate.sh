#!/bin/bash

PROJECT_DIR=`dirname $0`

curl -L http://git.frostnerd.com/dwolf/AndroidXMigrater/-/jobs/artifacts/master/download?job=build --output $PROJECT_DIR\migrate.zip
unzip -p $PROJECT_DIR\migrate.zip build/libs/androidxmigrater-1.0.jar > $PROJECT_DIR\androidxmigrater.jar && rm $PROJECT_DIR\migrate.zip

java -jar $PROJECT_DIR\androidxmigrater.jar $PROJECT_DIR
rm $PROJECT_DIR\androidxmigrater.jar
rm androidxmigrater.jar