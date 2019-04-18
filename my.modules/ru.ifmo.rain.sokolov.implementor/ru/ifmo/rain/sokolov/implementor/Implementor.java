package ru.ifmo.rain.sokolov.implementor;

import info.kgeorgiy.java.advanced.implementor.Impler;
import info.kgeorgiy.java.advanced.implementor.ImplerException;
import info.kgeorgiy.java.advanced.implementor.JarImpler;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
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
import java.util.jar.Attributes;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;

/**
 * Class for generation implementations of given abstract classes or interfaces
 * public Methods
 * <ul>
 * <li>{@link Implementor#implement(Class, Path)} generates implementation source code and outputs it to .java file</li>
 * <li>{@link Implementor#implement(Class, Path)} generates implementation source code and packs it to .jar archive </li>
 * </ul>
 * <p>
 * implements {@link JarImpler}
 *
 * @author Donat Sokolov
 * @version 1.0
 * @see JarImpler
 * @since 1.0
 */
public class Implementor implements Impler, JarImpler {

    private final static String EMPTY_STRING = "";
    private final static String EOLN = System.lineSeparator();
    private final static String SPACE = " ";
    private final static String COMMA = ",";

    private final static String DEF_OBJECT = " null";
    private final static String DEF_PRIMITIVE = " 0";
    private final static String DEF_VOID = EMPTY_STRING;
    private final static String DEF_BOOLEAN = " false";

    /**
     * Instantiates a new {@link Implementor} object
     */
    public Implementor() {
    }

    /**
     * Creates a {@link String} consisting of given <code>list</code>'s elements with <code>transform</code> function applied
     * joined by ", "
     *
     * @param list     list to be joined
     * @param <T>      type of elements in <code>list</code>
     * @param function function to be applied to all elements of the given <code>list</code>
     * @return joined sequence of transformed elements separated by ", "
     */
    private static <T> String joinToString(List<T> list, Function<T, String> function) {
        return list
                .stream()
                .map(function)
                .collect(Collectors.joining(COMMA + SPACE));
    }

    /**
     * Generates a throw-statement for given method or constructor
     *
     * @param method executable that can be either {@link Method} or {@link Constructor}
     * @return String containing text of throw-statement for the given <code>method</code> that consists of
     * <ul>
     * <li>throws keyword</li>
     * <li> list of fully-qualified names of all the Exceptions that can be thrown by given <code>method</code> separated by ", " </li>
     * </ul>
     */
    private static String getException(Executable method) {
        var exceptions = Arrays.asList(method.getExceptionTypes());
        return exceptions.isEmpty() ? EMPTY_STRING : "throws" + SPACE + joinToString(exceptions, Class::getCanonicalName);
    }

    /**
     * Returns the argument list of the given method. Method supports 2 modes
     * <ul>
     * <li> typed mode : arguments are following their types</li>
     * <li> untyped mode : arguments are printed without any types </li>
     * </ul>
     *
     * @param method Executable that can be either {@link Method} or {@link Constructor} - method to generate arguments list of
     * @param typed  indicates whether should be typed or not
     * @return String containing text of arguments list of the given <code>method</code> that consists of
     * <ul>
     * <li> list of <code>method</code>'s argument names separated by ", " for untyped mode </li>
     * <li> list of <code>method</code>'s argument types fully-qualified names followed by <code>method</code>'s argument names separated by ", " for untyped mode  </li>
     * </ul>
     */
    private static String getArguments(Executable method, boolean typed) {
        return joinToString(Arrays
                        .stream(method.getParameters())
                        .map(arg -> (typed ? arg.getType().getCanonicalName() + SPACE : EMPTY_STRING) + arg.getName())
                        .collect(Collectors.toList()),
                Function.identity()
        );
    }

    /**
     * Generates default implementation source code of the given <code>method</code> assuming that it is a method or a constructor
     * of a class with name = <code>newClassName</code>.
     * Implementation is generated to be correct implementation of the given method and formatted using Oracle's java code style
     *
     * @param method       method or constructor to generate implementation of
     * @param newClassName name of class containing the given <code>method</code>
     * @return <code>method</code>'s default implementation.
     * If the given <code>method</code> is a method then the body of it consists of a single return-statement.
     * Return value is the default value of <code>method</code>'s return type. If <code>method</code>'s return type is void
     * then return statement has no return value
     * If the given <code>method</code> is a constructor then the body of it consists of a single super constructor invocation statement.
     * Current <code>method</code>'s arguments are delegated to <code>super</code>
     */
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
        var modifiers = Modifier.toString(mods) + (mods != 0 ? " " : "");
        var arguments = getArguments(method, true);
        var exception = getException(method);
        var body = method instanceof Method ? "return" + returnTypeVal + ";"
                : "super(" + getArguments(method, false) + ");";

        return modifiers + "   " + returnTypeName + "(" + arguments + ") " + exception
                + "{" + EOLN
                + "        " + body + EOLN
                + "   }" + EOLN;
    }

    /**
     * Returns package name of given class
     *
     * @param token Class to get package
     * @return {@code token.getPackage().getName()} if {@code token.getPackage() != null} or empty string otherwise
     */
    private static String packageNameFor(Class<?> token) {
        return token.getPackage() != null ? token.getPackage().getName() : EMPTY_STRING;
    }

    /**
     * Returns name of default implementer name of given class or interface <code>token</code>
     *
     * @param token Class to get impl-name
     * @return simple name of given <code>tt</code> token followed by "Impl" suffix
     */
    private static String implNameFor(Class<?> token) {
        return token.getSimpleName() + "Impl";
    }

    /**
     * Provides command line interface for <code>ru.ifmo.rain.sokolov.implementor.Implementor</code> class.
     * Available methods: {@link Implementor#implement(Class, Path)} and {@link Implementor#implementJar(Class, Path)}
     * <p>
     *
     * @param args :
     *             <ul>
     *             <li> (1) class name, output path </li>
     *             <li> (2) "-jar", class name, output path </li>
     *             </ul>
     *             <p>
     *             Whether if {@link Class} for given class name cannot be loaded or if given an incorrect <code>args</code> array
     *             or if invoked method (<code>implement</code> or <code>implementJar</code>) fails
     *             then prints corresponding output messages
     *             <p>
     *             When given arguments(1) invokes {@link Implementor#implement(Class, Path)}
     *             When given arguments(2) invokes {@link Implementor#implementJar(Class, Path)}
     */
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
                    new Implementor().implement(Class.forName(args[0]), Paths.get(args[1]));
                } else if (!"-jar".equals(args[0])) {
                    System.err.println("Invalid argument usage: only option available is '-jar'");
                } else {
                    new Implementor().implementJar(Class.forName(args[1]), Paths.get(args[2]));
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
    }

    /**
     * Adds signatures of given <code>methods</code> to specified <code>destination</code>
     *
     * @param methods      array of {@link Method} to be added to destination
     * @param signatureSet {@link HashSet} of {@link MethodSignature} to add signatures of <code>methods</code>
     */
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

    /**
     * Converts given string to unicode escaping
     *
     * @param text {@link String} to convert
     * @return converted string
     */
    private static String toUnicode(String text) {
        StringBuilder out = new StringBuilder();
        for (char c : text.toCharArray()) {
            if (c >= 128) {
                out.append(String.format("\\u%04X", (int) c));
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    /**
     * Generates correct implementation source code of given <code>token</code> and produces coresponding .java file to given <code>root</code>
     * Produced implementation consists of single class with name <code>token</code>'s name + "Impl" suffix.
     * Impl-class has all default single-statement implementations of all required methods and constructors to be implemented.
     *
     * @param token {@link Class} to be implemented
     * @param root  path to directory for output
     * @throws ImplerException in the following situations:
     *                         <ul>
     *                         <li> <code>token</code> is <code>null</code> or <code>root</code> is <code>null</code> </li>
     *                         <li> <code>token</code> represents either a primitive type, final class, enum or array
     *                         (i.e. <code>token</code>) cannot be implemented </li>
     *                         <li> <code>root</code> is incorrect path </li>
     *                         <li> Error occurs while either creating of output file (with corresponding directories)
     *                         or writing anything to output file </li>
     *                         </ul>
     * @see Implementor#getMethodImpl(Executable, String)
     */
    @Override
    public void implement(Class<?> token, Path root) throws ImplerException {
        if (root == null || token == null)
            throw new ImplerException("Invalid argument: null value");

        if (token.isPrimitive() || token.isArray() || Modifier.isFinal(token.getModifiers()) || token == Enum.class) {
            throw new ImplerException("Incorrect token");
        }
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

            StringBuffer resultBuffer = new StringBuffer();
            if (!packageName.isEmpty()) {
                resultBuffer
                        .append("package" + SPACE)
                        .append(packageName)
                        .append(";")
                        .append(EOLN);
            }
            resultBuffer
                    .append("class" + SPACE)
                    .append(className).append(SPACE).append(token.isInterface() ? "implements" : "extends").append(SPACE)
                    .append(token.getCanonicalName())
                    .append(SPACE + "{")
                    .append(EOLN);
            implementMethodSignatures(token).forEach(method -> resultBuffer.append(method.toString()));

            if (!token.isInterface()) {
                addConstructors(token.getDeclaredConstructors(), className, resultBuffer);
            }

            resultBuffer.append("}").append(EOLN);
            var result = toUnicode(resultBuffer.toString());
            try {
                writer.write(result);
            } catch (IOException | SecurityException e) {
                throw new ImplerException("Failed to write to output file", e);
            }
        } catch (IOException | SecurityException e) {
            throw new ImplerException("Failed to create output file", e);
        }
    }

    /**
     * Returns package location relative path.
     *
     * @param token type token to create implementation for
     * @return a {@link String} representation of package relative path
     */
    private String getFilePath(Class<?> token) {
        return token.getPackageName().replace('.', File.separatorChar);
    }

    /**
     * Compiles given <code>file</code> and produces .class file to given <code>root</code> path
     *
     * @param token type token to create implementation for
     * @param root  path to output
     * @throws ImplerException if either failed to find system {@link JavaCompiler} or compiler returned non-null exit code
     */
    private void compileFiles(Class<?> token, Path root) throws ImplerException {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            throw new ImplerException("Not found java compiler");
        }

        Path originPath;
        try {
            var codeSource = token.getProtectionDomain().getCodeSource();
            var uri = codeSource == null ? "" : codeSource.getLocation().getPath();
            if (uri.startsWith("/")) {
                uri = uri.substring(1);
            }
            originPath = Path.of(uri);
        } catch (InvalidPathException e) {
            throw new ImplerException(String.format("Not found valid class path: %s", e));
        }
        String[] cmdArgs = new String[]{
                "-cp",
                root.toString() + File.pathSeparator + System.getProperty("java.class.path")
                        + File.pathSeparator + originPath.toString(),
                Path.of(root.toString(), getFilePath(token), implNameFor(token) + ".java").toString()
        };
        int exitCode = compiler.run(null, null, null, cmdArgs);
        if (exitCode != 0) {
            throw new ImplerException("Compilation ended with not zero code " + exitCode);
        }
    }

    /**
     * Builds a <code>.jar</code> file containing compiled by {@link #compileFiles(Class, Path)}
     * sources of implemented class using basic {@link Manifest}.
     *
     * @param jarFile       path where resulting <code>.jar</code> should be saved
     * @param tempDirectory temporary directory where all <code>.class</code> files are stored
     * @param token         type token that was implemented
     * @throws ImplerException if {@link JarOutputStream} processing throws an {@link IOException}
     */
    private void buildJar(Path jarFile, Path tempDirectory, Class<?> token) throws ImplerException {
        Manifest manifest = new Manifest();
        manifest.getMainAttributes().put(Attributes.Name.MANIFEST_VERSION, "1.0");

        try (JarOutputStream stream = new JarOutputStream(Files.newOutputStream(jarFile), manifest)) {
            String rootlessPath = token.getName().replace('.', '/') + "Impl.class";
            stream.putNextEntry(new ZipEntry(rootlessPath));
            Files.copy(Paths.get(tempDirectory.toString(), rootlessPath), stream);
        } catch (IOException e) {
            throw new ImplerException(e.getMessage());
        }
    }

    /**
     * Generates correct implementation source code of given <code>token</code> and produces a jar archive to given <code>jarFile</code> path
     * Produced implementation consists of single class with name <code>token</code>'s name + "Impl" suffix.
     * Impl-class has all default single-statement implementations of all required methods and constructors to be implemented.
     *
     * @param token   {@link Class} to be implemented
     * @param jarFile path to directory for output
     * @throws ImplerException in the following situations:
     *                         <ul>
     *                         <li> <code>token</code> is <code>null</code> or <code>root</code> is <code>null</code> </li>
     *                         <li> <code>token</code> represents either a primitive type, final class, enum or array
     *                         (i.e. <code>token</code>) cannot be implemented </li>
     *                         <li> <code>root</code> is incorrect path </li>
     *                         <li> Error occurs while either creating of output file (with corresponding directories)
     *                         or writing anything to output file </li>
     *                         </ul>
     * @see Implementor#getMethodImpl(Executable, String)
     */
    @Override
    public void implementJar(Class<?> token, Path jarFile) throws ImplerException {
        if (token == null || jarFile == null) {
            throw new ImplerException("Invalid argument given");
        }
        ImplementorFileUtils.createDirectoriesTo(jarFile.normalize());
        ImplementorFileUtils utils = new ImplementorFileUtils(jarFile.toAbsolutePath().getParent());
        try {
            Path root = utils.getTempDir();

            implement(token, root);
            compileFiles(token, root);
            buildJar(jarFile, root, token);
        } finally {
            utils.cleanTempDirectory();
        }
    }

    /**
     * Wrapper class for standard {@link Method} with overridden {@link MethodSignature#hashCode()} and {@link MethodSignature#equals(Object)}
     */
    class MethodSignature {

        private final static int BASE = 127;
        private final static int MOD = 793877113;

        /**
         * Method to be wrapped
         */
        private final Method method;

        /**
         * Instantiates new {@link MethodSignature} wrapping the given <code>method</code>
         *
         * @param other method to wrap
         */
        MethodSignature(Method other) {
            method = other;
        }

        /**
         * Get's name of wrapped <code>method</code>
         *
         * @return delegated to {@link Method#getName()} of <code>method</code>
         */
        private String getName() {
            return method.getName();
        }

        /**
         * Get's arguments of wrapped <code>method</code>
         *
         * @return delegated to {@link Method#getParameterTypes()} of <code>method</code>
         */
        private Class<?>[] getArgs() {
            return method.getParameterTypes();
        }

        /**
         * Generates the default implementation source code of wrapped <code>method</code>
         *
         * @return delegated to static {@link Implementor#getMethodImpl(Executable, String)}
         */
        @Override
        public String toString() {
            return Implementor.getMethodImpl(method, null);
        }

        /**
         * Overrides {@link Object#hashCode()}
         *
         * @return hash code value for current {@link MethodSignature}
         */
        @Override
        public int hashCode() {
            return (getName().hashCode() * BASE % MOD
                    + Arrays.hashCode(getArgs())) % MOD;
        }

        /**
         * Overrides {@link Object#equals(Object)}
         *
         * @param obj object to be compared with
         * @return <ul>
         * <li> <code>true</code> if <code>obj</code> is an instance of {@link MethodSignature} and values returned by
         * {@link MethodSignature#getName()} and {@link MethodSignature#getArgs()}
         * of both <code>this</code> and <code>obj</code> equals. </li>
         * <li> <code>false</code>, otherwise </li>
         * </ul>
         */
        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof MethodSignature)) {
                return false;
            }
            var other = (MethodSignature) obj;
            return Objects.equals(getName(), other.getName())
                    && Arrays.equals(getArgs(), other.getArgs());
        }
    }
}