package ru.ifmo.rain.sokolov.walk;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

class HashCounter {
    private static final int FNV_PRIME = 0x01000193;
    private static final int FNV_START = 0x811c9dc5;
    private static final int FNV_AND = 0xff;
    private static byte[] buffer = new byte[1024];

    static int getFNV1Hash(Path file) {
        int hash = FNV_START;
        try (var inputStream = new FileInputStream(file.toString())) {
            int sz;
            while ((sz = inputStream.read(buffer, 0, 1024)) != -1) {
                for (int i = 0; i < sz; ++i) {
                    hash = (hash * FNV_PRIME) ^ (buffer[i] & FNV_AND);
                }
            }
        } catch (IOException e) {
            hash = 0;
        }
        return hash;
    }
}
