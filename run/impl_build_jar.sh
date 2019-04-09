#!/usr/bin/env bash

mod_name=ru.ifmo.rain.sokolov.implementor
mod_path=ru/ifmo/rain/sokolov/implementor

wd=/home/damm1t/IdeaProjects/java-advanced-2019
out=$wd/out/production/java-advanced-2019
req=$wd/lib:$wd/artifacts

src=$wd/my.modules/$mod_name
run=$wd/run

javac --module-path $req $src/module-info.java $src/$mod_path/*.java -d $out
cd $out
jar -c --file=$run/Implementor.jar --main-class=$mod_name.Implementor --module-path=$req module-info.class $mod_path/*.class
cd $run