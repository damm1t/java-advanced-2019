package ru.ifmo.rain.sokolov.walk;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

public class RecursiveWalk {
    public static void main(String[] args) {
        if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
            System.out.println("invalid arguments\nExpected arguments: <input file> <output file>");
            return;
        }
        Path inputFile, outputFile;
        try {
            inputFile = Paths.get(args[0]);
            outputFile = Paths.get(args[1]);
        } catch (InvalidPathException e) {
            System.out.println("Invalid paths arguments (" + e.getMessage() + ")");
            return;
        }
        try (
                var input = Files.newBufferedReader(inputFile);
                var output = new PrintWriter(Files.newBufferedWriter(outputFile));
        ) {
            final int[] line = {0};
            final String format = "%08x %s" + System.lineSeparator();
            input.lines().forEach(curPath -> {
                line[0]++;
                try {
                    Files.walkFileTree(Paths.get(curPath), new SimpleFileVisitor<Path>() {
                        @Override
                        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                            output.printf(format, HashCounter.getFNV1Hash(file), file.toString());
                            if (output.checkError()) {
                                System.out.println("Output failed");
                            }
                            return FileVisitResult.CONTINUE;
                        }

                        @Override
                        public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
                            output.printf(format, 0, file.toString());
                            if (output.checkError()) {
                                System.out.println("Output failed");
                            }
                            return FileVisitResult.SKIP_SUBTREE;
                        }
                    });
                } catch (IOException | SecurityException e) {
                    System.out.println("Failed to read file from " + curPath + " at line " + line[0]);
                } catch (InvalidPathException e) {
                    System.out.println("Invalid path (at file " + curPath + " on line " + line[0] + ")");
                }
            });
        } catch (IOException | SecurityException e) {
            System.out.println("Failed to open input/output files" + e.getMessage() + ")");
        }
    }
}
