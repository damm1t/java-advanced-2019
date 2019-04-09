#!/bin/bash
java -p ../artifacts:../lib: -classpath ../out/production/java-advanced-2019 -m info.kgeorgiy.java.advanced.mapper list ru.ifmo.rain.sokolov.mapper.ParallelMapperImpl,ru.ifmo.rain.sokolov.mapper.IterativeParallelism
