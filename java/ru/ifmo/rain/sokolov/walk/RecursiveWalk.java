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
        try {
            inputPath = Paths.get(input);
            outputPath = Paths.get(output);
        } catch (InvalidPathException e) {
            throw new PathException("Invalid paths arguments (" + e.getMessage() + ")");
        }
        if (outputPath.getParent() != null && Files.notExists(outputPath.getParent())) {
            try {
                Files.createDirectories(outputPath.getParent());
            } catch (IOException e) {
                throw new PathException("Can't create output file (" + e.getMessage() + ")");
            }
        }
    }

    private static void walker(String path, BufferedWriter writer) throws IOException {
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
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                throw new WalkException("Invalid arguments\nWrong arguments: <input file> <output file>");
            }
            new RecursiveWalk(args[0], args[1]).walk();
        } catch (WalkException | PathException e) {
            System.out.println(e.getMessage());
        }
    }
}
