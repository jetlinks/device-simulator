#!/usr/bin/env bash
mvn clean package -DskipTests && clear && java -Dfile.encoding=UTF-8 -Xmx1G -Dsimulator.max-ports=50000 -jar ${PWD}/simulator-cli/target/simulator-cli.jar