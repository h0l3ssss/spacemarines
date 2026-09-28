package com.warhammer.towerdefense.data;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Reads data files straight from a directory. Used by tests and by the asset generator. */
public final class FileSystemDataSource implements DataSource {
    private final Path root;

    public FileSystemDataSource(Path root) {
        this.root = root;
    }

    @Override
    public String read(String path) {
        Path file = root.resolve(path);
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new DataException("Cannot read data file '" + file + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Files.isRegularFile(root.resolve(path));
    }

    public Path root() {
        return root;
    }
}
