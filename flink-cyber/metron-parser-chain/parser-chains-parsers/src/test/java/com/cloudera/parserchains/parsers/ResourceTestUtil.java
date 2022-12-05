package com.cloudera.parserchains.parsers;

import java.io.File;

public class ResourceTestUtil {

    static File getFileFromResource(String path) {
        return new File(ResourceTestUtil.class.getResource(path).getFile());
    }

}
