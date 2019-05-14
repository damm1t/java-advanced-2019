#!/bin/bash
java -p ../artifacts:../lib: -classpath ../out/production/java-advanced-2019 -m info.kgeorgiy.java.advanced.hello server-i18n ru.ifmo.rain.sokolov.helloudp.HelloUDPServer $1
