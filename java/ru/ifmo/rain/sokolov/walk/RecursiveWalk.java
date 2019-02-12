package ru.ifmo.rain.sokolov.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class RecursiveWalk {
    private final Path inputPath;
    private final Path outputPath;

    public RecursiveWalk(String input, String output) throws WalkException {
        try {
            inputPath = Paths.get(input);
            outputPath = Paths.get(output);
        } catch (InvalidPathException e) {
            throw new WalkException("Invalid paths arguments (" + e.getMessage() + ")");
        }
    }

    private static void walker(String path, PrintWriter writer) throws IOException {
        try {
            Path filePath = Paths.get(path);
            try {
                Files.walkFileTree(filePath, new Visitor(writer));
            } catch (IOException e) {
                writer.printf(Visitor.format, 0, filePath);
            }
        } catch (InvalidPathException e) {
            writer.printf(Visitor.format, 0, path);
        }
    }

    private void walk() throws WalkException {

        try (var bufferedReader = new BufferedReader(new InputStreamReader(
                new FileInputStream(inputPath.toString()), StandardCharsets.UTF_8))
        ) {
            try (var writer = new PrintWriter(new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(outputPath.toString()), StandardCharsets.UTF_8)))
            ) {
                try {
                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        try {
                            walker(line, writer);
                        } catch (IOException e) {
                            throw new WalkException("Failed to write to file from \\" + outputPath);
                        }
                    }
                } catch (IOException e) {
                    throw new WalkException("Failed to read file from \\" + inputPath);
                }
            } catch (IOException | SecurityException e) {
                throw new WalkException("Failed to open output file" + e.getMessage() + ")");
            }
        } catch (IOException | SecurityException e) {
            throw new WalkException("Failed to open input file" + e.getMessage() + ")");
        }
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                throw new WalkException("Invalid arguments\nWrong arguments: <input file> <output file>");
            }
            new RecursiveWalk(args[0], args[1]).walk();
        } catch (WalkException e) {
            System.out.println(e.getMessage());
        }
    }
}
