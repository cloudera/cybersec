package com.cloudera.cyber.generator;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nimbusds.jose.util.IOUtils;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import org.junit.Assert;
import org.junit.Test;

public class GeneratorUtilsTest {

    private static final String EXPECTED_FILE_CONTENTS = "this is a test";

    @Test
    public void testStreamFile() throws IOException {
        String dir = "src/test/resources/config";
        String file = "test_file.txt";
        verifyStreamFile(dir, file, EXPECTED_FILE_CONTENTS);
        verifyStreamFile("", dir.concat("/").concat(file), EXPECTED_FILE_CONTENTS);
        assertThatThrownBy(() -> verifyStreamFile("/doesnt_exist", file, null)).isInstanceOf(
              FileNotFoundException.class).hasMessage("Basedir: '/doesnt_exist' File: 'test_file.txt'");
    }

    private void verifyStreamFile(String baseDir, String file, String expectedResult) throws IOException {
        String result;
        try (InputStream stream = Utils.openFileStream(baseDir, file)) {
            result = IOUtils.readInputStreamToString(stream, Charset.defaultCharset());
        }
        Assert.assertEquals(expectedResult, result);
    }
}
