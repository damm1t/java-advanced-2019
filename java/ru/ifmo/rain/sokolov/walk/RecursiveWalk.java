package ru.ifmo.rain.sokolov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    private final Path inputPath;
    private final Path outputPath;

    public RecursiveWalk(String input, String output) throws PathException {
        inputPath = getPath(input, "Invalid input path: ");
        outputPath = getPath(output, "Invalid output path: ");
        Path parent = outputPath.getParent();
        if (parent != null && Files.notExists(parent)) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw new PathException("Can't create output file (" + e.getMessage() + ")");
            }
        }
    }

    private static Path getPath(String path, String message) throws PathException {
        try {
            return Paths.get(path);
        } catch (InvalidPathException e) {
            throw new PathException(message + e.getMessage());
        }
    }

    private void walker(String path, BufferedWriter writer) throws IOException {
        var visitor = new Visitor(writer);
        try {
            Files.walkFileTree(Paths.get(path), visitor);
        } catch (InvalidPathException e) {
            visitor.write(0, path);
        }
    }

    private void walk() throws WalkException {

        try (var reader = Files.newBufferedReader(inputPath)) {
            try (var writer = Files.newBufferedWriter(outputPath)) {
                try {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        try {
                            walker(line, writer);
                        } catch (IOException e) {
                            System.err.println("Failed write to \\" + outputPath);
                        }
                    }
                } catch (IOException e) {
                    throw new WalkException("Failed to read file from \\" + inputPath);
                }
            } catch (IOException | SecurityException e) {
                throw new WalkException("Failed to open output file " + e.getMessage() + ")");
            }
        } catch (IOException | SecurityException e) {
            throw new WalkException("Failed to open input file " + e.getMessage() + ")");
        }
    }

    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.err.println("Invalid arguments\nWrong arguments: <input file> <output file>");
            return;
        }
        try {

            new RecursiveWalk(args[0], args[1]).walk();
        } catch (WalkException | PathException e) {
            System.out.println(e.getMessage());
        }
    }
}
