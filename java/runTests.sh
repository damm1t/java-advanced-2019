#!/usr/bin/env bash
rmiregistry &
java -p ../lib: -classpath ../out/production/java-advanced-2019 ru.ifmo.rain.sokolov.rmi.BankTest