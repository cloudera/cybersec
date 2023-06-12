package com.cloudera.cyber.generator;

import freemarker.cache.TemplateLoader;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;

@Slf4j
public class FlinkFileTemplateLoader implements TemplateLoader {
    private final String basePath;

    public FlinkFileTemplateLoader(String basePath) {
        this.basePath = basePath;
    }

    @Override
    public Object findTemplateSource(String templateFile) throws IOException {
        Path templatePath = new Path(basePath, templateFile);
        FileSystem fileSystem = templatePath.getFileSystem();
        try {
            if (fileSystem.exists(templatePath) && !fileSystem.getFileStatus(templatePath).isDir()) {
                return templatePath;
            }
        } catch (Exception e) {
            log.error(String.format("Could not read template %s", templatePath.toString()), e);
        }
        return null;
    }

    @Override
    public long getLastModified(Object templatePathObject) {
        if (!(templatePathObject instanceof Path)) {
            throw new IllegalArgumentException("templatePathObject wasn't a flink Path, but a: " + templatePathObject.getClass().getName());
        }

        Path templatePath = (Path)templatePathObject;

        try {
            return templatePath.getFileSystem().getFileStatus(templatePath).getModificationTime();
        } catch (IOException e) {
            log.error(String.format("Could not stat template %s", templatePath.toString()), e);
        }

        return 0;
    }

    @Override
    public Reader getReader(Object templatePathObject, String encoding) throws IOException {
        Path templatePath = safePathConversion(templatePathObject);

        return new InputStreamReader(templatePath.getFileSystem().open(templatePath), encoding);
    }

    @Override
    public void closeTemplateSource(Object o) {

    }

    private Path safePathConversion(Object pathObject) {
        if (pathObject instanceof Path) {
            return (Path) pathObject;
        } else {
            throw new IllegalArgumentException(String.format("templatePathObject expected %s, but got %s ", Path.class, pathObject.getClass().getName()));
        }

    }

}
