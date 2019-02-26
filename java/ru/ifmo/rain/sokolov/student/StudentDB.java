package ru.ifmo.rain.sokolov.student;

import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;
import info.kgeorgiy.java.advanced.student.StudentGroupQuery;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.ToIntFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements StudentGroupQuery {

    private static final Comparator<Student> comparatorByName = Comparator
            .comparing(Student::getLastName, String::compareTo)
            .thenComparing(Student::getFirstName, String::compareTo)
            .thenComparingInt(Student::getId);

    private Stream<Group> groupsStreamByComparator(
            Collection<Student> students,
            Comparator<Student> studentComparator
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
                                .sorted(studentComparator)
                                .collect(Collectors.toList()))
                );
    }

    private List<Group> getGroupsWithComparator(Collection<Student> students,
                                                Comparator<Student> studentComparator) {
        return groupsStreamByComparator(students, studentComparator)
                .sorted(Comparator.comparing(Group::getName))
                .collect(Collectors.toList());
    }

    private Optional<Group> getLargestGroupComparingByStudentsList(Collection<Student> students,
                                                                   Function<List<Student>, Integer> studentsCmp) {
        return groupsStreamByComparator(students, comparatorByName).min(
                Comparator
                        .comparing((Group g) -> studentsCmp.apply(g.getStudents()))
                        .reversed()
                        .thenComparing(Group::getName)
        );
    }

    private static final String EMPTY_STRING = "";

    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return getGroupsWithComparator(students, comparatorByName);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return getGroupsWithComparator(students, Comparator.comparing(Student::getId));
    }


    private String getFilteredLargestGroup(Stream<Entry<String, List<Student>>> groupsStream,
                                           ToIntFunction<List<Student>> filter) {
        return groupsStream
                .max(Comparator.comparingInt((Entry<String, List<Student>> group) -> filter.applyAsInt(group.getValue()))
                        .thenComparing(Entry::getKey, Collections.reverseOrder(String::compareTo)))
                .map(Entry::getKey)
                .orElse(EMPTY_STRING);
    }

    private Stream<Entry<String, List<Student>>> getAnyGroupsStream(Collection<Student> students,
                                                                    Supplier<Map<String, List<Student>>> mapSupplier) {
        return students
                .stream()
                .collect(Collectors.groupingBy(Student::getGroup, mapSupplier, Collectors.toList()))
                .entrySet().stream();
    }

    private Stream<Entry<String, List<Student>>> getGroupsStream(Collection<Student> students) {
        return getAnyGroupsStream(students, HashMap::new);
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

    private <C extends Collection<String>> C mapCollection(
            Supplier<C> collection, Collection<Student> students, Function<Student, String> function) {
        return students
                .stream()
                .map(function)
                .collect(Collectors.toCollection(collection));
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

   /* private List<Student> abstractFind(Collection<Student> students, Predicate<Student> wtf) { //ToDO make normal name
        return abstractSortStudents(students.stream(), wtf);
    }*/

    private List<Student> filterStudentsByField(Collection<Student> students, Function<Student, String> field, String name) {
        return students
                .stream()
                .filter(s -> Objects.equals(field.apply(s), name))
                .sorted(comparatorByName)
                .collect(Collectors.toList());
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterStudentsByField(students, Student::getFirstName, name);
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterStudentsByField(students, Student::getLastName, name);
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
}
