package ru.ifmo.rain.sokolov.walk;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.UncheckedIOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;

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

    public void doWork() throws WalkException {

        try (var input = Files.newBufferedReader(inputPath);
             var output = new PrintWriter(Files.newBufferedWriter(outputPath))) {
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
                            System.err.println("sooooqa");
                            output.printf(format, 0, file.toString());
                            if (output.checkError()) {
                                System.out.println("Output failed");
                            }
                            return FileVisitResult.CONTINUE;
                        }
                    });
                } catch (InvalidPathException e) {
                    throw new WalkException("Invalid path (at file " + curPath + " on line " + line[0] + ")");

                } catch (IOException | SecurityException e) {
                    throw new WalkException("Failed to read file from \" + curPath + \" at line \" + line[0]");
                }
            });
        } catch (UncheckedIOException | IOException | SecurityException e) {
            throw new WalkException("Failed to open input/output files" + e.getMessage() + ")");
        }
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length != 2 || args[0] == null || args[1] == null) {
                throw new WalkException("invalid arguments\nExpected arguments: <input file> <output file>");
            }
            var dude = new RecursiveWalk(args[0], args[1]);
            dude.doWork();
        } catch (WalkException e) {
            System.out.println(e.getMessage());
        }

    }
}
