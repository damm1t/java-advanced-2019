#!/usr/bin/env bash

wd=/home/damm1t/IdeaProjects/java-advanced-2019
run=$wd/run

req=$run:$wd/lib:$wd/artifacts:

java --module-path $req -m ru.ifmo.rain.sokolov.implementor $1 $2 $3