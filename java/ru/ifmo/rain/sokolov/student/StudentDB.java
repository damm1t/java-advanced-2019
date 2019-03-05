package ru.ifmo.rain.sokolov.student;

import info.kgeorgiy.java.advanced.student.AdvancedStudentGroupQuery;
import info.kgeorgiy.java.advanced.student.Group;
import info.kgeorgiy.java.advanced.student.Student;

import java.util.*;
import java.util.Map.Entry;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class StudentDB implements AdvancedStudentGroupQuery {

    private final String EMPTY_STRING = "";

    private Comparator<Student> defaultStudentComparator = Comparator
            .comparing(Student::getLastName)
            .thenComparing(Student::getFirstName)
            .thenComparing(Student::getId);

    private <T, C extends Collection<T>> C mapToCollection(Collection<Student> students,
                                                           Function<Student, T> mapper,
                                                           Supplier<C> collector) {
        return students
                .stream()
                .map(mapper)
                .collect(Collectors.toCollection(collector));
    }

    private <T> List<T> mapToFieldsList(Collection<Student> students, Function<Student, T> mapper) {
        return mapToCollection(students, mapper, ArrayList::new);
    }

    private Stream<Student> sortedToStream(Stream<Student> students, Comparator<Student> comparator) {
        return students.sorted(comparator);
    }

    private List<Student> sortedToList(Collection<Student> students, Comparator<Student> comparator) {
        return sortedToStream(students.stream(), comparator)
                .collect(Collectors.toList());
    }

    private List<Student> filterAndSortToList(Collection<Student> students, Predicate<Student> predicate) {
        return sortedToStream(students.stream().filter(predicate), defaultStudentComparator)
                .collect(Collectors.toList());
    }

    private Predicate<Student> groupPredicate(String group) {
        return student -> Objects.equals(student.getGroup(), group);
    }

    private Stream<Entry<String, List<Student>>> groupToSortedEntriesStream(Stream<Student> students,
                                                                            Comparator<Student> comparator) {
        return groupToEntriesStream(sortedToStream(students, comparator));
    }

    private List<Group> sortToGroupList(Collection<Student> students, Comparator<Student> comparator) {
        return groupToSortedEntriesStream(students.stream(), comparator)
                .map(entry -> new Group(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(Group::getName, String::compareTo))
                .collect(Collectors.toList());
    }

    private Stream<Entry<String, List<Student>>> groupToEntriesStream(Stream<Student> students) {
        return students
                .collect(Collectors.groupingBy(Student::getGroup, LinkedHashMap::new, Collectors.toList()))
                .entrySet()
                .stream();
    }

    private String getFullName(Student student) {
        return student.getFirstName() + " " + student.getLastName();
    }

    private String getLargestGroupBy(Stream<Entry<String, List<Student>>> groups,
                                     Comparator<List<Student>> comparator) {
        return groups
                .max(Entry.<String, List<Student>>comparingByValue(comparator)
                        .thenComparing(Entry.comparingByKey(Collections.reverseOrder(String::compareTo))))
                .map(Entry::getKey)
                .orElse(EMPTY_STRING);
    }

    /// LAST MODIFICATION
    @Override
    public String getMostPopularName(Collection<Student> students) {
        return students.stream()
                .collect(Collectors.groupingBy(
                        this::getFullName,
                        Collectors.mapping(Student::getGroup, Collectors.toSet()))
                ).entrySet().stream()
                .max(Entry.<String, Set<String>>comparingByValue(Comparator.comparingInt(Set::size)).thenComparing(
                        Entry.comparingByKey(String::compareTo)))
                .map(Entry::getKey)
                .orElse(EMPTY_STRING);
    }

    /// HARD MODIFICATION
    @Override
    public List<Group> getGroupsByName(Collection<Student> students) {
        return sortToGroupList(students, defaultStudentComparator);
    }

    @Override
    public List<Group> getGroupsById(Collection<Student> students) {
        return sortToGroupList(students, Student::compareTo);
    }

    @Override
    public String getLargestGroup(Collection<Student> students) {
        return getLargestGroupBy(groupToEntriesStream(students.stream()),
                Comparator.comparingInt(List::size));
    }

    @Override
    public String getLargestGroupFirstName(Collection<Student> students) {
        return getLargestGroupBy(groupToEntriesStream(students.stream()),
                Comparator.comparingInt(list -> getDistinctFirstNames(list).size()));
    }

    /// EASY MODIFICATION
    @Override
    public List<String> getFirstNames(List<Student> students) {
        return mapToFieldsList(students, Student::getFirstName);
    }

    @Override
    public List<String> getLastNames(List<Student> students) {
        return mapToFieldsList(students, Student::getLastName);
    }

    @Override
    public List<String> getGroups(List<Student> students) {
        return mapToFieldsList(students, Student::getGroup);
    }

    @Override
    public List<String> getFullNames(List<Student> students) {
        return mapToFieldsList(students, this::getFullName);
    }

    @Override
    public Set<String> getDistinctFirstNames(List<Student> students) {
        return mapToCollection(students, Student::getFirstName, TreeSet::new);
    }

    @Override
    public String getMinStudentFirstName(List<Student> students) {
        return students.stream()
                .min(Student::compareTo)
                .map(Student::getFirstName)
                .orElse(EMPTY_STRING);
    }

    @Override
    public List<Student> sortStudentsById(Collection<Student> students) {
        return sortedToList(students, Student::compareTo);
    }

    @Override
    public List<Student> sortStudentsByName(Collection<Student> students) {
        return sortedToList(students, defaultStudentComparator);
    }

    @Override
    public List<Student> findStudentsByFirstName(Collection<Student> students, String name) {
        return filterAndSortToList(students, student -> Objects.equals(student.getFirstName(), name));
    }

    @Override
    public List<Student> findStudentsByLastName(Collection<Student> students, String name) {
        return filterAndSortToList(students, student -> Objects.equals(student.getLastName(), name));
    }

    @Override
    public List<Student> findStudentsByGroup(Collection<Student> students, String group) {
        return filterAndSortToList(students, groupPredicate(group));
    }

    @Override
    public Map<String, String> findStudentNamesByGroup(Collection<Student> students, String group) {
        return students.stream().filter(groupPredicate(group))
                .collect(Collectors.toMap(
                        Student::getLastName,
                        Student::getFirstName,
                        BinaryOperator.minBy(String::compareTo)
                ));
    }
}
