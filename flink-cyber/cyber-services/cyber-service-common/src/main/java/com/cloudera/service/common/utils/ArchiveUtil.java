package com.cloudera.service.common.utils;

import lombok.experimental.UtilityClass;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream;
import org.apache.commons.compress.compressors.gzip.GzipCompressorOutputStream;
import org.apache.flink.core.fs.FileStatus;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

@Slf4j
@UtilityClass
public class ArchiveUtil {

    public static void compressToTarGzFile(String inputPath, String outputPath) throws IOException {
        try (OutputStream fOut = Files.newOutputStream(Paths.get(outputPath))) {
            compressToTarGz(inputPath, fOut);
        }
    }

    public static byte[] compressToTarGzInMemory(String inputPath) throws IOException {
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            compressToTarGz(inputPath, bos);
            return bos.toByteArray();
        }
    }

    private static void compressToTarGz(String inputPath, OutputStream outputStream) throws IOException {
        final List<FileStatus> fileList = FileUtil.listFiles(inputPath, true);
        if (fileList == null || fileList.isEmpty()) {
            return;
        }

        try (BufferedOutputStream buffOut = new BufferedOutputStream(outputStream);
             GzipCompressorOutputStream gzOut = new GzipCompressorOutputStream(buffOut);
             TarArchiveOutputStream tOut = new TarArchiveOutputStream(gzOut)) {

            try {
                for (FileStatus file : fileList) {
                    addFileToTar(tOut, file, inputPath);
                }
            } finally {
                tOut.finish();
            }
        }
    }

    private static void addFileToTar(TarArchiveOutputStream tOut, FileStatus file, String rootPath) throws IOException {
        final Path filePath = Paths.get(file.getPath().getPath());
        String pathInsideTar;
        if (filePath.startsWith(rootPath)) {
            pathInsideTar = filePath.toString().substring(rootPath.length());
            while (pathInsideTar.startsWith("/")) {
                pathInsideTar = pathInsideTar.substring(1);
            }
        } else {
            pathInsideTar = filePath.toString();
        }

        TarArchiveEntry tarEntry = new TarArchiveEntry(
                filePath.toFile(),
                pathInsideTar);

        tOut.putArchiveEntry(tarEntry);
        Files.copy(filePath, tOut);
        tOut.closeArchiveEntry();
    }

    public static void decompressFromTarGzFile(String pathToTar, String outputPath) throws IOException {
        final Path path = Paths.get(pathToTar);
        if (Files.notExists(path)) {
            throw new IOException(String.format("File [%s] doesn't exists!", pathToTar));
        }
        try (InputStream fi = Files.newInputStream(path)) {
            decompressFromTarGz(fi, outputPath);
        }
    }

    public static void decompressFromTarGzInMemory(byte[] rawData, String outputPath) throws IOException {
        if (rawData == null) {
            throw new IOException("Provided null as .tar.gz data which is not allowed!");
        }
        try (InputStream bi = new ByteArrayInputStream(rawData)) {
            decompressFromTarGz(bi, outputPath);
        }
    }

    public static void decompressFromTarGz(InputStream inputStream, String outputPath) throws IOException {
        try (BufferedInputStream bi = new BufferedInputStream(inputStream);
             GzipCompressorInputStream gzi = new GzipCompressorInputStream(bi);
             TarArchiveInputStream ti = new TarArchiveInputStream(gzi)) {

            ArchiveEntry entry;
            while ((entry = ti.getNextEntry()) != null) {
                if (entry.isDirectory()) {
                    continue;
                }

                File curfile = new File(outputPath, entry.getName());
                File parent = curfile.getParentFile();
                if (!parent.exists()) {
                    parent.mkdirs();
                }
                Files.copy(ti, curfile.toPath(), StandardCopyOption.REPLACE_EXISTING);
            }
        }
    }

}
