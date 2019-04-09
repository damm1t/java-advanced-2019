#!/usr/bin/env bash

wd=/home/damm1t/IdeaProjects/java-advanced-2019
run=$wd/run

req=$run:$wd/lib:$wd/artifacts


java --module-path $req --add-modules ru.ifmo.rain.sokolov.implementor -m info.kgeorgiy.java.advanced.implementor $1 ru.ifmo.rain.sokolov.implementor.Implementor $2
