package com.cloudera.service.common.utils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.experimental.UtilityClass;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

@UtilityClass
public class FileUtil {

    public static List<FileStatus> listFiles(String path, boolean recursive) throws IOException {
        return listFiles(new Path(path), recursive);
    }

    public static List<FileStatus> listFiles(Path path, boolean recursive) throws IOException {
        final FileSystem fileSystem = path.getFileSystem();
        if (!fileSystem.exists(path)) {
            return null;
        }
        final FileStatus[] statusList = fileSystem.listStatus(path);
        if (statusList == null) {
            return null;
        }

        List<FileStatus> result = new ArrayList<>();

        for (FileStatus fileStatus : statusList) {
            if (fileStatus.isDir() && recursive) {
                final List<FileStatus> recursiveStatusList = listFiles(fileStatus.getPath(), recursive);
                if (recursiveStatusList != null) {
                    result.addAll(recursiveStatusList);
                }
            } else {
                result.add(fileStatus);
            }
        }
        return result;
    }

}
