#!/usr/bin/env bash
mvn clean package -DskipTests && clear && java -jar ${PWD}/simulator-cli/target/simulator-cli.jar