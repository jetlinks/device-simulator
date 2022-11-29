#!/usr/bin/env bash
mvn clean package -DskipTests && clear && java -Xmx1G -Dsimulator.max-ports=2000 -jar ${PWD}/simulator-cli/target/simulator-cli.jar