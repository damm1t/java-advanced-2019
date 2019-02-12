package ru.ifmo.rain.sokolov.walk;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;

class HashCounter {
    private static final int FNV_FACTOR = 0x01000193;
    private static final int FNV_START = 0x811c9dc5;
    private static final int FNV_AND = 0xff;
    private static final int BUF_SIZE = 1024;
    private static byte[] buf = new byte[BUF_SIZE];

    static int getFNV1Hash(Path path) {
        int hash = FNV_START;
        try (var inputStream = new FileInputStream(path.toString())) {
            int sz;
            while ((sz = inputStream.read(buf, 0, BUF_SIZE)) != -1) {
                for (int i = 0; i < sz; ++i) {
                    hash = (hash * FNV_FACTOR) ^ (buf[i] & FNV_AND);
                }
            }
        } catch (IOException e) {
            hash = 0;
        }
        return hash;
    }
}
