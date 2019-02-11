# Тесты к курсу «Технологии Java»

[Условия домашних заданий](http://www.kgeorgiy.info/courses/java-advanced/homeworks.html)

## Домашнее задание 1. Обход файлов

Для того, чтобы протестировать программу:

 * Скачайте тесты ([WalkTest.jar](artifacts/WalkTest.jar)) и библиотеки к ним:
    [junit-4.11.jar](lib/junit-4.11.jar), [hamcrest-core-1.3.jar](lib/hamcrest-core-1.3.jar)
 * Откомпилируйте решение домашнего задания
 * Протестируйте домашнее задание
    * простой вариант:
        ```info.kgeorgiy.java.advanced.walk.Tester Walk <полное имя класса>```
    * сложный вариант:
        ```info.kgeorgiy.java.advanced.walk.Tester RecursiveWalk <полное имя класса>```
 * `CLASSPATH` должен
 	* содержать все скачанные `.jar` файлы;
 	* содержать скомпилированное решение;
 	* __не__ содержать скомпилированные самостоятельно тесты.

Исходный код тестов:

 * [простой вариант](java/info/kgeorgiy/java/advanced/walk/WalkTest.java)
 * [сложный вариант](java/info/kgeorgiy/java/advanced/walk/RecursiveWalkTest.java)
