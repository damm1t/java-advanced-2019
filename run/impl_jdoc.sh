#!/usr/bin/env bash

wd=/home/damm1t/IdeaProjects/java-advanced-2019

mod_name=ru.ifmo.rain.sokolov.implementor
mod_path=ru/ifmo/rain/sokolov/implementor
res_path=$wd/modules/info.kgeorgiy.java.advanced.implementor/info/kgeorgiy/java/advanced/implementor
run=$wd/run

out=$wd/out/production/java-advanced-2019

req=$run:$wd/lib:$wd/artifacts:$wd/my.modules

src=$wd/my.modules/$mod_name


javadoc --class-path $req --module-path $req -link https://docs.oracle.com/en/java/javase/11/docs/api/ --show-module-contents api --show-packages exported --show-types private --show-members private --expand-requires transitive --module-source-path $wd/my.modules:$wd/modules:$wd/my.modules -d $out/scripted/docs --module $mod_name $res_path/Impler.java $res_path/JarImpler.java $res_path/ImplerException.java

