package org.jboss.pnc.causeway.source;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import com.redhat.red.build.koji.model.json.StandardOutputType;

import lombok.Getter;

public class RenamedSources {
    private final Path file;
    @Getter
    private final int size;
    @Getter
    private final String name;
    @Getter
    private final String md5;
    @Getter
    private final StandardOutputType type;
    private boolean read = false;

    public RenamedSources(Path file, String name, String md5, StandardOutputType type) throws IOException {
        this.file = file;
        this.name = name;
        this.md5 = md5;
        this.size = (int) Files.size(file);
        this.type = type;
    }

    public InputStream read() throws IOException {
        if (read) {
            throw new IllegalStateException("File already read.");
        }
        read = true;
        return Files.newInputStream(file, StandardOpenOption.DELETE_ON_CLOSE);
    }
}
