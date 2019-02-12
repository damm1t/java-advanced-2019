package ru.ifmo.rain.sokolov.walk;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Visitor extends SimpleFileVisitor<Path> {
    private PrintWriter writer;
    static final String format = "%08x %s" + System.lineSeparator();

    public Visitor(PrintWriter writer) {
        this.writer = writer;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        writer.printf(format, HashCounter.getFNV1Hash(file), file.toString());
        if (writer.checkError()) {
            System.out.println("Output failed");
        }
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        writer.printf(format, 0, file.toString());
        if (writer.checkError()) {
            System.out.println("Output failed");
        }
        return FileVisitResult.CONTINUE;
    }

}