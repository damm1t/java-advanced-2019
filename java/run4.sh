#!/usr/bin/env bash
java -p ../../artifacts:../../lib: -classpath ../../out/production/java-advanced-2019 -m info.kgeorgiy.java.advanced.implementor class ru.ifmo.rain.sokolov.implementor.Implementor
java -p ../../artifacts:../../lib: -classpath ../../out/production/java-advanced-2019 -m info.kgeorgiy.java.advanced.implementor jar-class ru.ifmo.rain.sokolov.implementor.Implementor
java -p ../../artifacts:../../lib: -classpath . -m info.kgeorgiy.java.advanced.implementor jar-class ru.ifmo.rain.sokolov.implementor.Implementor