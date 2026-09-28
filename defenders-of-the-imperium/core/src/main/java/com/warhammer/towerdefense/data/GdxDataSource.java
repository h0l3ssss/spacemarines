package com.warhammer.towerdefense.data;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;

/** Reads data files through LibGDX internal file handles (working directory first, then classpath). */
public final class GdxDataSource implements DataSource {
    @Override
    public String read(String path) {
        FileHandle handle = Gdx.files.internal(path);
        if (!handle.exists()) {
            throw new DataException("Data file not found: " + path);
        }
        try {
            return handle.readString("UTF-8");
        } catch (RuntimeException e) {
            throw new DataException("Cannot read data file '" + path + "': " + e.getMessage(), e);
        }
    }

    @Override
    public boolean exists(String path) {
        return Gdx.files.internal(path).exists();
    }
}
