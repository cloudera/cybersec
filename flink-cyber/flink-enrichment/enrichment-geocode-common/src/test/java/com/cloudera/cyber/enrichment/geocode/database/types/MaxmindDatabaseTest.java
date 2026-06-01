package com.cloudera.cyber.enrichment.geocode.database.types;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class MaxmindDatabaseTest {

    /**
     * Creates a test tar file containing a mmdb file to test .tar.gz
     * geocode databases.
     *
     * @param mmdbFilePath The resource name of the mmdb file to include in the tar file.
     * @throws IOException If the file can't be created.
     */
    protected String createTarGzWithMmdb(Path tempDir, String mmdbFilePath) throws IOException {
        // Create a temporary .tar.gz file containing the .mmdb
        File mmdbFile = new File(mmdbFilePath);
        String tarFileName = Paths.get(mmdbFilePath).getFileName().toString().replace(".mmdb", ".tar.gz");
        File tarGzFile = tempDir.resolve(tarFileName).toFile();
        try (FileOutputStream fos = new FileOutputStream(tarGzFile);
             BufferedOutputStream bos = new BufferedOutputStream(fos);
             GzipCompressorOutputStream gcos = new GzipCompressorOutputStream(bos);
             TarArchiveOutputStream taos = new TarArchiveOutputStream(gcos)) {

            TarArchiveEntry entry = new TarArchiveEntry(mmdbFile, mmdbFile.getName());
            entry.setSize(mmdbFile.length());
            taos.putArchiveEntry(entry);

            try (FileInputStream fis = new FileInputStream(mmdbFile)) {
                fis.transferTo(taos);
            }

            taos.closeArchiveEntry();
            taos.finish();
        }
        assertTrue(tarGzFile.exists(), String.format("Temp '%s' file should exist", tarGzFile));
        return tarGzFile.getAbsolutePath();
    }
}
