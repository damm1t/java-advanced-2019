package ru.ifmo.rain.sokolov.walk;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class Visitor extends SimpleFileVisitor<Path> {
    private BufferedWriter writer;
    static final String format = "%08x %s";

    public Visitor(BufferedWriter writer) {
        this.writer = writer;
    }

    void write(int hash, String file) throws IOException {
        writer.write(String.format(format, hash, file));
        writer.newLine();
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        write(HashCounter.getFNV1Hash(file), file.toString());
        return FileVisitResult.CONTINUE;
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        write(0, file.toString());
        return FileVisitResult.SKIP_SUBTREE;
    }

}