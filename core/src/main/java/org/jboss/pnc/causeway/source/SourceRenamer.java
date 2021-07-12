package org.jboss.pnc.causeway.source;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.DigestOutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import javax.enterprise.context.ApplicationScoped;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.CompressorException;
import org.apache.commons.compress.compressors.CompressorStreamFactory;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.commons.io.IOUtils;
import org.jboss.pnc.causeway.CausewayException;

@ApplicationScoped
public class SourceRenamer {

    public static final String ARCHIVE_SUFFIX = "-project-sources.tar.gz";
    private CompressorStreamFactory compressor = new CompressorStreamFactory();

    /**
     * Repackage the sources archive and rename the root directory inside to match the following format:
     * {@code <artifactId>-<version>-project-sources.tar.gz}.
     */
    public RenamedSources repackMaven(InputStream input, String groupId, String artifactId, String version)
            throws CausewayException {
        String name = getMavenName(artifactId, version);
        Path path = getMavenPath(groupId, artifactId, version);
        return repack(input, name, path);
    }

    private Path getMavenPath(String groupId, String artifactId, String version) {
        String gid = groupId.replace(".", "/");
        return Paths.get(gid).resolve(artifactId).resolve(version);
    }

    private String getMavenName(String artifactId, String version) {
        return artifactId + "-" + version;
    }

    public String getMavenDeployPath(String groupId, String artifactId, String version) {
        return Paths.get("/")
                .resolve(getMavenPath(groupId, artifactId, version))
                .resolve(getMavenName(artifactId, version) + ARCHIVE_SUFFIX)
                .toString();
    }

    /**
     * Repackage the sources archive and rename the root directory inside to match the following format:
     * {@code <packageName>-<version>-project-sources.tar.gz}.
     */
    public RenamedSources repackNPM(InputStream input, String packageName, String version) throws CausewayException {
        String name = getNPMName(packageName, version);
        Path path = getNPMPath(packageName);
        return repack(input, name, path);
    }

    private Path getNPMPath(String packageName) {
        return Paths.get(packageName).resolve("-");
    }

    private String getNPMName(String packageName, String version) {
        return packageName + "-" + version;
    }

    public String getNPMDeployPath(String packageName, String version) {
        return Paths.get("/")
                .resolve(getNPMPath(packageName))
                .resolve(getNPMName(packageName, version) + ARCHIVE_SUFFIX)
                .toString();
    }

    private RenamedSources repack(InputStream input, String name, Path path) throws CausewayException {
        try {
            Path tempFile = Files.createTempFile("renamer-", ".tar.gz");

            MessageDigest md5Digest = MessageDigest.getInstance("MD5");
            DigestOutputStream outputStream = new DigestOutputStream(Files.newOutputStream(tempFile), md5Digest);
            rewrite(input, outputStream, name);

            BigInteger bi = new BigInteger(1, md5Digest.digest());
            String md5Hash = String.format("%032x", bi);

            String archiveName = name + ARCHIVE_SUFFIX;

            return new RenamedSources(tempFile, path.resolve(archiveName).toString(), md5Hash);
        } catch (IOException | CompressorException e) {
            throw new CausewayException("Error while repacking archive with changed root directory name", e);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("The JVM must support MD5 digest", e);
        }
    }

    private void rewrite(InputStream input, OutputStream output, String name) throws CompressorException, IOException {
        Path newDirectoryName = Paths.get(name);
        try (TarArchiveInputStream in = new TarArchiveInputStream(
                compressor.createCompressorInputStream(new BufferedInputStream(input)));
                TarArchiveOutputStream out = new TarArchiveOutputStream(new GzipCompressorOutputStream(output))) {
            out.setLongFileMode(TarArchiveOutputStream.LONGFILE_GNU);

            boolean rootFound = false;
            for (TarArchiveEntry entry = in.getNextTarEntry(); entry != null; entry = in.getNextTarEntry()) {
                Path originalName = Paths.get(entry.getName());
                Path root = getTopmost(originalName);
                if (root.equals(originalName)) { // validate only one directory exists in root of the archive
                    if (!entry.isDirectory()) {
                        throw new IllegalArgumentException("There is non-directory file in root of the archive.");
                    }
                    if (rootFound) {
                        throw new IllegalArgumentException("Multiple directories in root of the archive.");
                    }
                    rootFound = true;
                }
                Path newName = newDirectoryName.resolve(root.relativize(originalName));
                entry.setName(newName.toString());
                out.putArchiveEntry(entry);
                IOUtils.copy(in, out);
                out.closeArchiveEntry();
            }
        }
    }

    private Path getTopmost(Path path) {
        while (path.getParent() != null) {
            path = path.getParent();
        }
        return path;
    }

}
