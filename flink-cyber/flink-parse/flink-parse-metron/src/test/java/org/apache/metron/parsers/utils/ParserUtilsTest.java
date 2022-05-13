/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.parsers.utils;

import junit.framework.TestCase;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import java.text.ParseException;

public class ParserUtilsTest extends TestCase {

    public void testConvertToEpoch() throws ParseException {
        Boolean adjustTimezone = true;
        String[] timeToTest = {"Mar", "2", "05:24:39"};
        int year = Calendar.getInstance().get(Calendar.YEAR);
        String timeToTestWithYear = String.valueOf(year) + " " + timeToTest[0] + " " + timeToTest[1] + " " + timeToTest[2];
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy MMM d HH:mm:ss", Locale.ENGLISH);
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Date date = sdf.parse(timeToTestWithYear);
        Long expectedTs = date.getTime();
        Long ts = ParserUtils.convertToEpoch(timeToTest[0], timeToTest[1], timeToTest[2], adjustTimezone);
        assertEquals(expectedTs, ts);
    }
}
