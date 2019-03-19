package ru.ifmo.rain.sokolov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;
import ru.ifmo.rain.sokolov.walk.PathException;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Executable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class Implementor implements Impler, JarImpler {

    private final static String EMPTY_STRING = "";
    private final static String EOLN = System.lineSeparator();
    private final static String SPACE = " ";
    private final static String COMMA = ",";

    private final static String DEF_OBJECT = " null";
    private final static String DEF_PRIMITIVE = " 0";
    private final static String DEF_VOID = EMPTY_STRING;
    private final static String DEF_BOOLEAN = " false";

    private static <T> String joinToString(List<T> list, Function<T, String> function) {
        return list
                .stream()
                .map(function)
                .collect(Collectors.joining(COMMA + SPACE));
    }

    private static String getException(Executable method) {
        var exceptions = Arrays.asList(method.getExceptionTypes());
        return exceptions.isEmpty() ? EMPTY_STRING : "throws" + SPACE + joinToString(exceptions, Class::getCanonicalName);
    }

    private static String getArguments(Executable method, boolean typed) {
        return joinToString(Arrays
                        .stream(method.getParameters())
                        .map(arg -> (typed ? arg.getType().getCanonicalName() + SPACE : EMPTY_STRING) + arg.getName())
                        .collect(Collectors.toList()),
                Function.identity()
        );
    }

    private static String getMethodImpl(Executable method, String newClassName) {
        String returnTypeName;
        Class<?> returnType = null;

        if (method instanceof Method) {
            Method newMethod = (Method) method;
            returnType = newMethod.getReturnType();
            returnTypeName = returnType.getCanonicalName() + " " + newMethod.getName();
        } else {
            returnTypeName = newClassName;
        }
        String returnTypeVal = EMPTY_STRING;
        if (returnType != null) {
            returnTypeVal = returnType.isPrimitive() ?
                    (returnType.equals(boolean.class) ? DEF_BOOLEAN
                            : returnType.equals(void.class) ? DEF_VOID : DEF_PRIMITIVE)
                    : DEF_OBJECT;
        }
        final int mods = method.getModifiers() & ~Modifier.ABSTRACT & ~Modifier.TRANSIENT & ~Modifier.NATIVE;
        //Arrays %s  mods %s  returnName %s(args %s) throws %s {%n         method %s;%n   }%n"
        final String format = "%n    %s %s %s(%s) %s {%n        %s;%n   }%n";

        return String.format(format,
                Arrays
                        .stream(method.getAnnotations())
                        .map(Annotation::toString)
                        .collect(Collectors.joining(EOLN)),
                Modifier.toString(mods),
                returnTypeName,
                getArguments(method, true),
                getException(method),
                method instanceof Method
                        ? "return" + returnTypeVal
                        : "super(" + getArguments(method, false) + ")"
        );
    }

    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {

    }

    class MethodSignature {

        private final static int BASE = 127;
        private final static int MOD = 793877113;

        private final Method method;

        MethodSignature(Method other) {
            method = other;
        }

        private String getName() {
            return method.getName();
        }

        private Class<?> getReturnType() {
            return method.getReturnType();
        }

        private Class<?>[] getArgs() {
            return method.getParameterTypes();
        }

        @Override
        public String toString() {
            return Implementor.getMethodImpl(method, null);
        }

        @Override
        public int hashCode() {
            return ((getName().hashCode() * BASE * BASE % MOD
                    + Arrays.hashCode(getArgs()) * BASE) % MOD
                    + getReturnType().hashCode()) % MOD;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodSignature))
                return false;

            MethodSignature other = (MethodSignature) obj;
            return Objects.equals(getName(), other.getName())
                    && Objects.equals(getReturnType(), other.getReturnType())
                    && Arrays.equals(getArgs(), other.getArgs());
        }

    }

    private StringBuffer getHead(Class<?> token, String packageName, String className) {
        var resultBuffer = new StringBuffer();
        if (!packageName.isEmpty()) {
            resultBuffer
                    .append("package" + SPACE)
                    .append(packageName)
                    .append(";")
                    .append(EOLN);
        }
        return resultBuffer
                .append("class" + SPACE)
                .append(className).append(SPACE).append(token.isInterface() ? "implements" : "extends").append(SPACE)
                .append(token.getSimpleName())
                .append(SPACE + "{")
                .append(EOLN);
    }

    private void getAbstractMethods(Method[] methods, Set<MethodSignature> signatureSet) {
        Arrays
                .stream(methods)
                .filter(method -> Modifier.isAbstract(method.getModifiers()))
                .map(MethodSignature::new)
                .collect(Collectors.toCollection(() -> signatureSet));
    }

    private HashSet<MethodSignature> implementMethodSignatures(Class<?> token) {
        HashSet<MethodSignature> methods = new HashSet<>();
        getAbstractMethods(token.getMethods(), methods);
        while (token != null) {
            getAbstractMethods(token.getDeclaredMethods(), methods);
            token = token.getSuperclass();
        }
        return methods;
    }

    private void addConstructors(Constructor[] constructor, String className, StringBuffer output) throws ImplerException {
        List<Constructor> constructors = Arrays
                .stream(constructor)
                .filter(constr -> !Modifier.isPrivate(constr.getModifiers()))
                .collect(Collectors.toList());

        if (constructors.size() == 0) {
            throw new ImplerException("Error: no accessible constructors in class");
        }
        constructors.forEach(itConstructor -> output.append(getMethodImpl(itConstructor, className)));
    }

    private static String packageNameFor(Class<?> token) {
        return token.getPackage() != null ? token.getPackage().getName() : EMPTY_STRING;
    }

    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (root == null || token == null)
            throw new ImplerException("Invalid argument: null value");

        if (token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers()) || token == Enum.class)
            throw new ImplerException("Incorrect token");

        String packageName = packageNameFor(token);
        String className = token.getSimpleName() + "Impl";
        Path containingDirectory = root.resolve(packageName.replace('.', File.separatorChar));
        try {
            Files.createDirectories(containingDirectory);
        } catch (IOException e) {
            throw new ImplerException("Failed to create directory for output java-file", e);
        }
        Path resultPath = containingDirectory.resolve(className + ".java");
        try (BufferedWriter writer = Files.newBufferedWriter(resultPath)) {

            StringBuffer resultBuffer = getHead(token, packageName, className);

            implementMethodSignatures(token).forEach(method -> resultBuffer.append(method.toString()));

            if (!token.isInterface()) {
                addConstructors(token.getDeclaredConstructors(), className, resultBuffer);
            }

            resultBuffer.append("}").append(EOLN);
            var result = resultBuffer.toString();
            try {
                writer.write(result);
            } catch (IOException | SecurityException e) {
                throw new ImplerException("Failed to write to output file", e);
            }
        } catch (IOException | SecurityException e) {
            throw new ImplerException("Failed to create output file", e);
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length < 2 || args.length > 3) {
            System.err.println("Invalid arguments number");
        } else {
            for (String arg : args) {
                if (arg == null) {
                    System.err.println("All arguments should be not null");
                    return;
                }
            }
            try {
                if (args.length == 2) {
                    new Implementor().implement(Class.forName(args[0]), Path.of(args[1]));
                } else if (!"-jar".equals(args[0])) {
                    System.err.println("Invalid argument usage: only option available is '-jar'");
                } else {
                    new Implementor().implementJar(Class.forName(args[1]), Path.of(args[2]));
                }
            } catch (ClassNotFoundException e) {
                System.err.printf("Invalid class name given: %s\n", e.getMessage());
            } catch (InvalidPathException e) {
                System.err.printf("Invalid path given: %s\n", e.getMessage());
            } catch (ImplerException e) {
                System.err.printf("Error while creating %s file: %s\n", args.length == 2 ? "java" : "jar",
                        e.getMessage());
            }
        }
        /*if (args == null || args.length != 2) {
            System.err.println("Invalid arguments\nWrong arguments: <class name> <output file>");
            return;
        }

        try {
            new Implementor().implement(Class.forName(args[0]), Paths.get(args[1]));
        } catch (ClassNotFoundException e) {
            System.err.println("the class cannot be located " + e.getMessage());
        } catch (InvalidPathException e) {
            System.err.println("Invalid path " + args[1] + e.getMessage());
        } catch (ImplerException e) {
            System.err.println(e.getMessage());
        }*/
    }

}