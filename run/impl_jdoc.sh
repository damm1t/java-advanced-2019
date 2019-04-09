#!/usr/bin/env bash

wd=/home/damm1t/IdeaProjects/java-advanced-2019

mod_name=ru.ifmo.rain.sokolov.implementor
mod_path=ru/ifmo/rain/sokolov/implementor
res_name=info.kgeorgiy.java.advanced
res_path=$wd/modules/$res_name.implementor/info/kgeorgiy/java/advanced/implementor
run=$wd/run

out=$wd/out/production/java-2019
req=$wd/lib:$wd/artifacts:$run

src=$wd/my.modules/$mod_name

javadoc -d javadoc -link https://docs.oracle.com/en/java/javase/11/docs/api/ --module-path $req -private -version -author --module-source-path $wd/my.modules --module $mod_name $res_path/Impler.java $res_path/JarImpler.java $res_path/ImplerException.java
google-chrome --new-window $run/javadoc/index.html