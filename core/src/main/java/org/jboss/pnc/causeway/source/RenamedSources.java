package org.jboss.pnc.causeway.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import lombok.Getter;

public class RenamedSources {
    private final Path file;
    @Getter
    private final int size;
    @Getter
    private final String name;
    @Getter
    private final String md5;
    private boolean read = false;

    public RenamedSources(Path file, String name, String md5) throws IOException {
        this.file = file;
        this.name = name;
        this.md5 = md5;
        this.size = (int) Files.size(file);
    }

    public InputStream read() throws IOException {
        if (read) {
            throw new IllegalStateException("File already read.");
        }
        read = true;
        return Files.newInputStream(file, StandardOpenOption.DELETE_ON_CLOSE);
    }
}
