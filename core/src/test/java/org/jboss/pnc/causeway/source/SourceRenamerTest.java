package org.jboss.pnc.causeway.source;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.jboss.pnc.causeway.CausewayException;
import org.junit.Test;

import static org.jboss.pnc.causeway.source.SourceRenamer.ARCHIVE_SUFFIX;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SourceRenamerTest {

    private SourceRenamer renamer = new SourceRenamer();

    @Test
    public void shouldRenameRootDirectory() throws CausewayException, IOException {
        String groupId = "org.foo.bar";
        String artifactId = "foo-bar-utils";
        String version = "1.0.0.Final-redhat-00001";

        InputStream sources = SourceRenamerTest.class.getResourceAsStream("foobar.tar.gz");
        RenamedSources repack = renamer.repackMaven(sources, groupId, artifactId, version);
        String newName = artifactId + "-" + version;
        assertEquals(
                "org/foo/bar/foo-bar-utils/1.0.0.Final-redhat-00001/" + newName + ARCHIVE_SUFFIX,
                repack.getName());

        TarArchiveInputStream tarArchiveInputStream = new TarArchiveInputStream(
                new GzipCompressorInputStream(repack.read()));

        ArchiveEntry entry = tarArchiveInputStream.getNextEntry();
        int count = 0;
        while (entry != null) {
            count++;
            assertTrue(entry.getName().startsWith(newName));
            entry = tarArchiveInputStream.getNextEntry();
        }
        assertEquals(5, count);
    }
}
