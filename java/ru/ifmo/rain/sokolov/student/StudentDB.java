package ru.ifmo.rain.sokolov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    private static final Comparator<Student> comparatorByName = Comparator
            .comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName, String::compareTo)
            .thenComparingInt(Student::getId);

    private Stream<Group> groupsStreamByComparator(
            Collection<Student> students,
            Comparator<Student> comparator
    ) {
        return students
                .stream()
                .collect(Collectors.toMap(
                        Student::getGroup,
                        Stream::of,
                        Stream::concat
                ))
                .entrySet()
                .stream()
                .map(it -> new Group(
                        it.getKey(),
                        it.getValue()
                                .sorted(comparator)
                                .collect(Collectors.toList()))
                );
    }

    private List<Group> getGroupsByComparator(Collection<Student> students,
                                              Comparator<Student> comparator) {
        return groupsStreamByComparator(students, comparator)
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    private Stream<Entry<String, List<Student>>> getGroupsStream(Collection<Student> students,
                                                                 Supplier<Map<String, List<Student>>> mapSupplier) {
        return students
                .stream()
                .collect(Collectors.groupingBy(Student::getGroup, mapSupplier, Collectors.toList()))
                .entrySet().stream();
    }

    private List<Student> filterStudentsByFunc(Collection<Student> students, Function<Student, String> function, String name) {
        return students
                .stream()
                .filter(s -> Objects.equals(function.apply(s), name))
                .sorted(comparatorByName)
                .collect(Collectors.toList());
    }

    private <C extends Collection<String>> C mapCollection(
            Supplier<C> collection, Collection<Student> students, Function<Student, String> function) {
        return students
                .stream()
                .map(function)
                .collect(Collectors.toCollection(collection));
    }

    private static final String EMPTY_STRING = "";

    private String getFilteredLargestGroup(Stream<Entry<String, List<Student>>> groupsStream,
                                           ToIntFunction<List<Student>> filter) {
        return groupsStream
                .max(Comparator.comparingInt((Entry<String, List<Student>> group) -> filter.applyAsInt(group.getValue()))
                        .thenComparing(Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Entry::getKey)
                .orElse(EMPTY_STRING);
    }

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsByComparator(students, comparatorByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsByComparator(students, Comparator.comparing(Student::getId));
    }

    private Stream<Entry<String, List<Student>>> getGroupsStream(Collection<Student> students) {
        return getGroupsStream(students, TreeMap::new);
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getFilteredLargestGroup(getGroupsStream(students), List::size);
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getFilteredLargestGroup(getGroupsStream(students),
                studentsList -> getDistinctFirstNames(studentsList).size());
    }

    private List<String> mapList(Collection<Student> students, Function<Student, String> function) {
        return mapCollection(ArrayList::new, students, function);
    }

    private Set<String> mapSet(Collection<Student> students, Function<Student, String> function) {
        return mapCollection(TreeSet::new, students, function); //ToDo .distinct()
    }

    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapList(students, it -> it.getFirstName() + " " + it.getLastName());
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapSet(students, Student::getFirstName);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students
                .stream()
                .min(Student::compareTo)//Comparator.comparing(Student::getId)
                .map(Student::getFirstName)
                .orElse(EMPTY_STRING);
    }


    private List<Student> abstractSortStudents(Stream<Student> studentStream, Comparator<Student> cmp) {
        return studentStream.sorted(cmp).collect(Collectors.toList());
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return abstractSortStudents(students.stream(), Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return abstractSortStudents(students.stream(), comparatorByName);
    }



    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterStudentsByFunc(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterStudentsByFunc(students, Student::getLastName, name);
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return students
                .stream()
                .filter(it -> Objects.equals(it.getGroup(), group))
                .sorted(comparatorByName)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students
                .stream()
                .filter(it -> Objects.equals(it.getGroup(), group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)
                ));
    }

    @Override
    public String getMostPopularName(Collection<Student> students) {
        return students
                .stream()
                .collect(Collectors.toMap());
        /*return students
                .stream()
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)
                ))
                .;*/
    }
}
