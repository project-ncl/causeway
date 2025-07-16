/*
 * Copyright 2024 Red Hat, Inc.
 * SPDX-License-Identifier: Apache-2.0
 */
package org.jboss.pnc.causeway.impl;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.jboss.pnc.causeway.ErrorMessages;

import lombok.Getter;

public class BurnAfterReadingFile {
    protected final Path file;
    @Getter
    protected final int size;
    @Getter
    protected final String name;
    @Getter
    protected final String md5;
    private boolean read = false;

    public BurnAfterReadingFile(Path file, String name, String md5) throws IOException {
        this.file = file;
        this.size = (int) Files.size(file);
        this.name = name;
        this.md5 = md5;
    }

    public BurnAfterReadingFile(String name, long size, String md5) {
        this.file = null;
        this.size = (int) size;
        this.name = name;
        this.md5 = md5;
    }

    public static BurnAfterReadingFile fromInputStream(String name, InputStream is)
            throws IOException, NoSuchAlgorithmException {
        Path tempFile = Files.createTempFile("barf-", name);

        MessageDigest md5Digest = MessageDigest.getInstance("MD5");
        try (is; DigestOutputStream outputStream = new DigestOutputStream(Files.newOutputStream(tempFile), md5Digest)) {
            is.transferTo(outputStream);
        }

        BigInteger bi = new BigInteger(1, md5Digest.digest());
        String md5Hash = String.format("%032x", bi);

        return new BurnAfterReadingFile(tempFile, name, md5Hash);
    }

    public InputStream read() throws IOException {
        if (read) {
            throw new IllegalStateException(ErrorMessages.sourcesFileWasAlreadyRead());
        }
        read = true;
        return Files.newInputStream(file, StandardOpenOption.DELETE_ON_CLOSE);
    }
}
