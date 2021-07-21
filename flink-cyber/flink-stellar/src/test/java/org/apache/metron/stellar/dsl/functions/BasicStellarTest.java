/*
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

package org.apache.metron.stellar.dsl.functions;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import org.apache.commons.lang3.StringUtils;
import org.apache.metron.stellar.common.StellarProcessor;
import org.apache.metron.stellar.common.utils.StellarProcessorUtils;
import org.apache.metron.stellar.dsl.*;
import org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.*;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;


@SuppressWarnings("ALL")
public class BasicStellarTest {

  @Stellar(
          description="throw exception",
          name="THROW",
          params = {
           "message - exception message"
          },
          returns="nothing"
  )
  public static class Throw implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      throw new IllegalStateException(Joiner.on(" ").join(args));
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }

  }

  @Stellar(
          description="Always returns true",
          name="RET_TRUE",
          params = {
           "arg* - Any set of args you wish to give it (including the empty set), they're ignored."
          },
          returns="true"
  )
  public static class TrueFunc implements StellarFunction {

    @Override
    public Object apply(List<Object> args, Context context) throws ParseException {
      return true;
    }

    @Override
    public void initialize(Context context) {

    }

    @Override
    public boolean isInitialized() {
      return true;
    }

  }

  @Test
  public void ensureDocumentation() {
    ClassLoader classLoader = getClass().getClassLoader();
    int numFound = 0;
    for (Class<?> clazz : new ClasspathFunctionResolver().resolvables()) {
      if (clazz.isAnnotationPresent(Stellar.class)) {
        numFound++;
        Stellar annotation = clazz.getAnnotation(Stellar.class);
        assertFalse(StringUtils.isEmpty(annotation.name()), "Must specify a name for " + clazz.getName());
        assertFalse(StringUtils.isEmpty(annotation.description()), "Must specify a description annotation for " + clazz.getName());
        assertFalse(StringUtils.isEmpty(annotation.returns()), "Must specify a returns annotation for " + clazz.getName());
      }
    }
    assertTrue(numFound > 0);
  }

  @Test
  public void testEscapedLiterals() {
    assertEquals("'bar'", run("\"'bar'\"", new HashMap<>()));
    assertEquals("'BAR'", run("TO_UPPER('\\'bar\\'')", new HashMap<>()));
    assertEquals("\"bar\"", run("\"\\\"bar\\\"\"", new HashMap<>()));
    assertEquals("\"bar\"", run("'\"bar\"'", new HashMap<>()));
    assertEquals("\"BAR\"", run("TO_UPPER(\"\\\"bar\\\"\")", new HashMap<>()));
    assertEquals("bar \\ foo", run("'bar \\\\ foo'", new HashMap<>()));
    assertEquals("bar \\\\ foo", run("'bar \\\\\\\\ foo'", new HashMap<>()));
    assertEquals("bar\nfoo", run("'bar\\nfoo'", new HashMap<>()));
    assertEquals("bar\n\nfoo", run("'bar\\n\\nfoo'", new HashMap<>()));
    assertEquals("bar\tfoo", run("'bar\\tfoo'", new HashMap<>()));
    assertEquals("bar\t\tfoo", run("'bar\\t\\tfoo'", new HashMap<>()));
    assertEquals("bar\rfoo", run("'bar\\rfoo'", new HashMap<>()));
    assertEquals("'bar'", run("'\\'bar\\''", new HashMap<>()));
  }

  @Test
  public void testVariableResolution() {
    {
      String query = "bar:variable";
      assertEquals("bar", run(query, ImmutableMap.of("bar:variable", "bar")));
      assertEquals("grok", run(query, ImmutableMap.of("bar:variable", "grok")));
    }
    {
      String query = "JOIN(['foo', bar:variable], '')";
      assertEquals("foobar", run(query, ImmutableMap.of("bar:variable", "bar")));
      assertEquals("foogrok", run(query, ImmutableMap.of("bar:variable", "grok")));
    }
    {
      String query = "MAP_GET('bar', { 'foo' : 1, 'bar' : bar:variable})";
      assertEquals("bar", run(query, ImmutableMap.of("bar:variable", "bar")));
      assertEquals("grok", run(query, ImmutableMap.of("bar:variable", "grok")));
    }
  }

  @Test
  public void testMissingVariablesWithParse() {
    String query = "someVar";
    assertThrows(ParseException.class, () -> run(query,new HashMap<>()));
  }

  @Test
  public void testValidateDoesNotThrow(){
    String query = "someVar";
    validate(query);
  }

  @Test
  public void testContextActivityTypeReset(){
    String query = "someVar";
    Context context = Context.EMPTY_CONTEXT();

    validate(query,context);
    assertNull(context.getActivityType());

    run(query,ImmutableMap.of("someVar","someValue"),context);
    assertNull(context.getActivityType());


  }

  @Test
  public void testIfThenElseBug1() {
    String query = "50 + (true == true ? 10 : 20)";
    assertEquals(60, run(query, new HashMap<>()));
  }

  @Test
  public void testIfThenElseBug2() {
    String query = "50 + (true == false ? 10 : 20)";
    assertEquals(70, run(query, new HashMap<>()));
  }

  @Test
  public void testIfThenElseBug3() {
    String query = "50 * (true == false ? 2 : 10) + 20";
    assertEquals(520, run(query, new HashMap<>()));
  }

  @Test
  public void testIfThenElseBug4() {
    String query = "TO_INTEGER(true == true ? 10.0 : 20.0 )";
    assertEquals(10, run(query, new HashMap<>()));
  }

  @Test
  public void testVariablesUsed() {
    StellarProcessor processor = new StellarProcessor();
    {
      assertEquals(new HashSet<>(), processor.variablesUsed("if 1 < 2 then 'one' else 'two'"));
    }
    {
      assertEquals(ImmutableSet.of("one")
                         , processor.variablesUsed("if 1 < 2 then one else 'two'"));
    }
    {
      assertEquals(ImmutableSet.of("one", "two")
                         , processor.variablesUsed("if 1 < 2 then one else two"));
    }
    {
      assertEquals(ImmutableSet.of("bar")
                         , processor.variablesUsed("MAP_GET('foo', { 'foo' : bar})"));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testConditionalsAsMapKeys() {
    {
      String query = "{ ( RET_TRUE() && y < 50 ) : 'info', y >= 50 : 'warn'}";
      Map<Boolean, String> ret = (Map)run(query, ImmutableMap.of("y", 50));
      assertEquals(ret.size(), 2);
      assertEquals("warn", ret.get(true));
      assertEquals("info", ret.get(false));
    }
  }


  @Test
  public void testConditionalsAsFunctionArgs() {
    {
      String query = "RET_TRUE(y < 10)";
      assertTrue((boolean)run(query, ImmutableMap.of("y", 50)));
    }
  }

  @Test
  public void testFunctionEmptyArgs() {
    {
      String query = "STARTS_WITH(casey, 'case') or MAP_EXISTS()";
      assertTrue((Boolean)run(query, ImmutableMap.of("casey", "casey")));
    }
    {
      String query = "true or MAP_EXISTS()";
      assertTrue((Boolean)run(query, new HashMap<>()));
    }
    {
      String query = "MAP_EXISTS() or true";
      assertTrue((Boolean)run(query, new HashMap<>()));
    }
  }
  @Test
  public void testNull() {
    {
      String query = "if 1 < 2 then NULL else true";
      assertNull(run(query, new HashMap<>()));
    }
    {
      String query = "1 < 2 ? NULL : true";
      assertNull(run(query, new HashMap<>()));
    }
    {
      String query = "null == null ? true : false";
      assertTrue((Boolean)run(query, new HashMap<>()));
    }
  }

  @Test
  public void testNaN(){
    // any equity is false
    // https://docs.oracle.com/javase/specs/jls/se6/html/typesValues.html
    {
      String query = "NaN == NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "5.0 == NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NULL == NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "'metron' == NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }

    // any inequity is true
    {
      String query = "NaN != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "5 != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "'metron' != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }

    //  any > >= < <= is false
    {
      String query = "NaN > 5";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN < 5";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN >= 5";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN <= 5";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN > NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN < NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN >= NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN <= NaN";
      assertFalse(runPredicate(query,new HashMap<>()));
    }

    // all operations
    {
      String query = "(5 + NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "5 + NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
    {
      String query = "(5 - NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "5 - NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
    {
      String query = "(5 / NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "5 / NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
    {
      String query = "(5 * NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "5 * NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
    {
      String query = "(NaN + NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN + NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
    {
      String query = "(NaN - NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN - NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
    {
      String query = "(NaN * NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN * NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
    {
      String query = "(NaN / NaN) != NaN";
      assertTrue(runPredicate(query,new HashMap<>()));
    }
    {
      String query = "NaN / NaN";
      assertTrue(run(query,new HashMap<>()).toString().equals("NaN"));
    }
  }

  @Test
  public void testMapConstant() {
    {
      String query = "MAP_GET('bar', { 'foo' : 1, 'bar' : 'bar'})";
      assertEquals("bar", run(query, new HashMap<>()));
    }
    {
      String query = "MAP_GET('blah', {  'blah' : 1 < 2 })";
      assertEquals(true, run(query, new HashMap<>()));
    }
    {
      String query = "MAP_GET('blah', {  'blah' : not(STARTS_WITH(casey, 'case')) })";
      assertEquals(false, run(query, ImmutableMap.of("casey", "casey")));
    }
    {
      String query = "MAP_GET('blah', {  'blah' : one })";
      assertEquals(1, run(query, ImmutableMap.of("one", 1)));
    }
    {
      String query = "MAP_GET('blah', {  'blah' : null })";
      assertNull(run(query, new HashMap<>()));
    }
    {
      String query = "MAP_GET('BLAH', {  TO_UPPER('blah') : null })";
      assertNull(run(query, new HashMap<>()));
    }
    {
      String query = "MAP_GET('BLAH', {  TO_UPPER('blah') : 1 < 2 })";
      assertEquals(true, run(query, new HashMap<>()));
    }
  }

  @Test
  public void testIfThenElse() {
    {
      String query = "if STARTS_WITH(casey, 'case') then 'one' else 'two'";
      assertEquals("one", run(query, ImmutableMap.of("casey", "casey")));
    }
    {
      String query = "if 1 < 2 then 'one' else 'two'";
      assertEquals("one", run(query, new HashMap<>()));
    }
    {
      String query = "if 1 + 1 < 2 then 'one' else 'two'";
      assertEquals("two", run(query, new HashMap<>()));
    }
    {
      String query = "if 1 + 1 <= 2 AND 1 + 2 in [3] then 'one' else 'two'";
      assertEquals("one", run(query, new HashMap<>()));
    }
    {
      String query = "if 1 + 1 <= 2 AND (1 + 2 in [3]) then 'one' else 'two'";
      assertEquals("one", run(query, new HashMap<>()));
    }
    {
      String query = "if not(1 < 2) then 'one' else 'two'";
      assertEquals("two", run(query, new HashMap<>()));
    }
    {
      String query = "if 1 == 1.0000001 then 'one' else 'two'";
      assertEquals("two", run(query, new HashMap<>()));
    }
    {
      String query = "if one < two then 'one' else 'two'";
      assertEquals("one", run(query, ImmutableMap.of("one", 1, "two", 2)));
    }
    {
      String query = "if one == very_nearly_one then 'one' else 'two'";
      assertEquals("two", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "if one == very_nearly_one OR one == very_nearly_one then 'one' else 'two'";
      assertEquals("two", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "if one == very_nearly_one OR one != very_nearly_one then 'one' else 'two'";
      assertEquals("one", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "if one != very_nearly_one OR one == very_nearly_one then 'one' else 'two'";
      assertEquals("one", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "if 'foo' in ['foo'] OR one == very_nearly_one then 'one' else 'two'";
      assertEquals("one", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "if ('foo' in ['foo']) OR one == very_nearly_one then 'one' else 'two'";
      assertEquals("one", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "if not('foo' in ['foo']) OR one == very_nearly_one then 'one' else 'two'";
      assertEquals("two", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "if not('foo' in ['foo'] OR one == very_nearly_one) then 'one' else 'two'";
      assertEquals("two", run(query, ImmutableMap.of("one", 1, "very_nearly_one", 1.0000001)));
    }
    {
      String query = "1 < 2 ? 'one' : 'two'";
      assertEquals("one", run(query, new HashMap<>()));
    }
    {
      String query = "1 < 2 ? TO_UPPER('one') : 'two'";
      assertEquals("ONE", run(query, new HashMap<>()));
    }
    {
      String query = "1 < 2 ? one : 'two'";
      assertEquals("one", run(query, ImmutableMap.of("one", "one")));
    }
    {
      String query = "1 < 2 ? one*3 : 'two'";
      assertTrue(Math.abs(3 - (int) run(query, ImmutableMap.of("one", 1))) < 1e-6);
    }
    {
      String query = "1 < 2 AND 1 < 2 ? one*3 : 'two'";
      assertTrue(Math.abs(3 - (int) run(query, ImmutableMap.of("one", 1))) < 1e-6);
    }
    {
      String query = "1 < 2 AND 1 > 2 ? one*3 : 'two'";
      assertEquals("two", run(query, ImmutableMap.of("one", 1)));
    }
    {
      String query = "1 > 2 AND 1 < 2 ? one*3 : 'two'";
      assertEquals("two", run(query, ImmutableMap.of("one", 1)));
    }
    {
      String query = "1 < 2 AND 'foo' in ['', 'foo'] ? one*3 : 'two'";
      assertEquals(3, run(query, ImmutableMap.of("one", 1)));
    }
    {
      String query = "1 < 2 AND ('foo' in ['', 'foo']) ? one*3 : 'two'";
      assertEquals(3, run(query, ImmutableMap.of("one", 1)));
    }
    {
      String query = "'foo' in ['', 'foo'] ? one*3 : 'two'";
      assertEquals(3, run(query, ImmutableMap.of("one", 1)));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testInNotIN(){
    HashMap variables = new HashMap<>();
    boolean thrown = false;
    try{
      Object o = run("in in ['','in']" ,variables );
    }catch(ParseException pe) {
      thrown = true;
    }
    assertTrue(thrown);
    thrown = false;

    try{
      assertEquals(true,run("'in' in ['','in']" ,variables ));
    }catch(ParseException pe) {
      thrown = true;
    }
    assertFalse(thrown);
  }

  @Test
  public void testHappyPath() {
    String query = "TO_UPPER(TRIM(foo))";
    assertEquals("CASEY", run(query, ImmutableMap.of("foo", "casey ")));
  }

  @Test
  public void testLengthString(){
    String query = "LENGTH(foo)";
    assertEquals(5, run(query,ImmutableMap.of("foo","abcde")));
  }
  @Test
  public void testLengthCollection(){
    String query = "LENGTH(foo)";
    Collection c = Arrays.asList(1,2,3,4,5);
    assertEquals(5, run(query,ImmutableMap.of("foo",c)));
  }

  @Test
  public void testEmptyLengthString(){
    String query = "LENGTH(foo)";
    assertEquals(0,run(query,ImmutableMap.of("foo","")));
  }
  @Test
  public void testEmptyLengthCollection(){
    String query = "LENGTH(foo)";
    Collection c = new ArrayList();
    assertEquals(0,run(query,ImmutableMap.of("foo",c)));
  }
  @Test
  public void testNoVarLength(){
    String query = "LENGTH(foo)";
    assertThrows(ParseException.class, () -> run(query,ImmutableMap.of()));
  }

  @Test
  public void testJoin() {
    String query = "JOIN( [ TO_UPPER(TRIM(foo)), 'bar' ], ',')";
    assertEquals("CASEY,bar", run(query, ImmutableMap.of("foo", "casey ")));
    query = "JOIN( SET_INIT( [ 1, 2, 'buckle', 'my', 'shoe', 3 ] ), ',')";
    assertEquals("1,2,buckle,my,shoe,3", run(query, new HashMap<>()));
  }

  @Test
  public void testSplit() {
    String query = "JOIN( SPLIT(foo, ':'), ',')";
    assertEquals("casey,bar", run(query, ImmutableMap.of("foo", "casey:bar")));
  }

  @Test
  public void testMapGet() {
    String query = "MAP_GET(dc, dc2tz, 'UTC')";
    assertEquals("UTC"
                       , run(query, ImmutableMap.of("dc", "nyc"
                                                   ,"dc2tz", ImmutableMap.of("la", "PST")
                                                   )
                            )
                       );
    assertEquals("EST"
                       , run(query, ImmutableMap.of("dc", "nyc"
                                                   ,"dc2tz", ImmutableMap.of("nyc", "EST")
                                                   )
                            )
                       );
  }

  @Test
  public void testMapPut() {
    Map vars = ImmutableMap.of("mymap", new HashMap<String, String>());
    String query = "MAP_PUT('foo','bar',mymap)";
    assertThat(run(query, vars), instanceOf(Map.class));
    query = "MAP_GET('foo', mymap)";
    assertThat(run(query, vars), equalTo("bar"));
  }

  @Test
  public void testMapPutDefault() {
    Map vars = new HashMap() {{
      put("mymap", null);
    }};
    String query = "MAP_PUT('foo','bar', mymap)";
    Map result = (Map) run(query, vars);
    assertThat(result, instanceOf(Map.class));
    assertThat(result.size(), equalTo(1));
    assertThat(result.get("foo"), equalTo("bar"));
  }

  @Test
  public void mapPutTest_wrongType() throws Exception {
    assertThrows(ParseException.class, () -> run("MAP_PUT( 'foo', 'bar', [ 'baz' ] )", new HashMap<>()));
  }

  @Test
  public void testMapMergeEmpty() {
    Map m = (Map) StellarProcessorUtils.run("MAP_MERGE([{}, null])", new HashMap<>());
    assertEquals(0, m.size());
  }

  @Test
  public void testMapMergeFromVariables() {
    Map vars = new HashMap() {{
      put("map1", ImmutableMap.of("a", 1, "b", 2));
      put("map2", ImmutableMap.of("c", 3, "d", 4));
      put("map3", ImmutableMap.of("e", 5, "f", 6));
    }};
    String query = "MAP_MERGE([map1, map2, map3])";
    Map result = (Map) run(query, vars);
    assertThat(result, instanceOf(Map.class));
    assertThat(result.size(), equalTo(6));
    assertThat(result.get("a"), equalTo(1));
    assertThat(result.get("b"), equalTo(2));
    assertThat(result.get("c"), equalTo(3));
    assertThat(result.get("d"), equalTo(4));
    assertThat(result.get("e"), equalTo(5));
    assertThat(result.get("f"), equalTo(6));
  }

  @Test
  public void testMapMergeSingleMap() {
    String query = "MAP_MERGE( [ { 'a' : '1', 'b' : '2', 'c' : '3' } ] )";
    Map result = (Map) run(query, new HashMap<>());
    assertThat(result, instanceOf(Map.class));
    assertThat(result.size(), equalTo(3));
    assertThat(result.get("a"), equalTo("1"));
    assertThat(result.get("b"), equalTo("2"));
    assertThat(result.get("c"), equalTo("3"));
  }

  @Test
  public void testMapMergeFromInlineMaps() {
    String query = "MAP_MERGE( [ { 'a' : '1', 'b' : '2' }, { 'c' : '3', 'd' : '4' }, { 'e' : '5', 'f' : '6' } ] )";
    Map result = (Map) run(query, new HashMap<>());
    assertThat(result, instanceOf(Map.class));
    assertThat(result.size(), equalTo(6));
    assertThat(result.get("a"), equalTo("1"));
    assertThat(result.get("b"), equalTo("2"));
    assertThat(result.get("c"), equalTo("3"));
    assertThat(result.get("d"), equalTo("4"));
    assertThat(result.get("e"), equalTo("5"));
    assertThat(result.get("f"), equalTo("6"));
  }

  @Test
  public void testMapMergeWithOverlappingMapsAndMixedTypes() {
    String query = "MAP_MERGE( [ { 'a' : '1', 'b' : 2, 'c' : '3' }, { 'c' : '3b', 'd' : '4' }, { 'd' : '4b', 'e' : 5, 'f' : '6' } ] )";
    Map result = (Map) run(query, new HashMap<>());
    assertThat(result, instanceOf(Map.class));
    assertThat(result.size(), equalTo(6));
    assertThat(result.get("a"), equalTo("1"));
    assertThat(result.get("b"), equalTo(2));
    assertThat(result.get("c"), equalTo("3b"));
    assertThat(result.get("d"), equalTo("4b"));
    assertThat(result.get("e"), equalTo(5));
    assertThat(result.get("f"), equalTo("6"));
  }

  @Test
  public void mapMergeTest_wrongType() throws Exception {
    assertThrows(ParseException.class, () -> run("MAP_MERGE( [ 'foo', 'bar' ] )", new HashMap<>()));
  }

  @Test
  public void testTLDExtraction() {
    String query = "DOMAIN_TO_TLD(foo)";
    assertEquals("co.uk", run(query, ImmutableMap.of("foo", "www.google.co.uk")));
  }

  @Test
  public void testTLDRemoval() {
    String query = "DOMAIN_REMOVE_TLD(foo)";
    assertEquals("www.google", run(query, ImmutableMap.of("foo", "www.google.co.uk")));
  }

  @Test
  public void testSubdomainRemoval() {
    String query = "DOMAIN_REMOVE_SUBDOMAINS(foo)";
    assertEquals("google.co.uk", run(query, ImmutableMap.of("foo", "www.google.co.uk")));
    assertEquals("google.com", run(query, ImmutableMap.of("foo", "www.google.com")));
  }

  @Test
  public void testURLToHost() {
    String query = "URL_TO_HOST(foo)";
    assertEquals("www.google.co.uk", run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Test
  public void testURLToPort() {
    String query = "URL_TO_PORT(foo)";
    assertEquals(80, run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Test
  public void testURLToProtocol() {
    String query = "URL_TO_PROTOCOL(foo)";
    assertEquals("http", run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Test
  public void testURLToPath() {
    String query = "URL_TO_PATH(foo)";
    assertEquals("/my/path", run(query, ImmutableMap.of("foo", "http://www.google.co.uk/my/path")));
  }

  @Disabled //until field transformations avail to Stellar
  @Test
  public void testProtocolToName() {
    String query = "PROTOCOL_TO_NAME(protocol)";
    assertEquals("TCP", run(query, ImmutableMap.of("protocol", "6")));
    assertEquals("TCP", run(query, ImmutableMap.of("protocol", 6)));
    assertEquals(null, run(query, ImmutableMap.of("foo", 6)));
    assertEquals("chicken", run(query, ImmutableMap.of("protocol", "chicken")));
  }

  @Test
  public void testDateConversion() {
    long expected =1452013350000L;
    {
      String query = "TO_EPOCH_TIMESTAMP(foo, 'yyyy-MM-dd HH:mm:ss', 'UTC')";
      assertEquals(expected, run(query, ImmutableMap.of("foo", "2016-01-05 17:02:30")));
    }
    {
      String query = "TO_EPOCH_TIMESTAMP(foo, 'yyyy-MM-dd HH:mm:ss')";
      Long ts = (Long) run(query, ImmutableMap.of("foo", "2016-01-05 17:02:30"));
      //is it within 24 hours of the UTC?
      assertTrue(Math.abs(ts - expected) < 8.64e+7);
    }
  }

  @Test
  public void testToString() {
    assertEquals("5", run("TO_STRING(foo)", ImmutableMap.of("foo", 5)));
  }

  @Test
  public void testToStringNull() {
    assertEquals("null", run("TO_STRING(\"null\")", ImmutableMap.of("foo", "null")));
  }

  @Test
  public void testToInteger() {
    assertEquals(5, run("TO_INTEGER(foo)", ImmutableMap.of("foo", "5")));
    assertEquals(5, run("TO_INTEGER(foo)", ImmutableMap.of("foo", 5)));
  }

  @Test
  public void testToDouble() {
    assertEquals(5.1d, run("TO_DOUBLE(foo)", ImmutableMap.of("foo", 5.1d)));
    assertEquals(5.1d, run("TO_DOUBLE(foo)", ImmutableMap.of("foo", "5.1")));
  }

  @Test
  public void testGet() {
    Map<String, Object> variables = ImmutableMap.of("foo", "www.google.co.uk");
    assertEquals("www", run("GET_FIRST(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'))", variables));
    assertEquals("www", run("GET(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'), 0)", variables));
    assertEquals("google", run("GET_LAST(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'))", variables));
    assertEquals("google", run("GET(SPLIT(DOMAIN_REMOVE_TLD(foo), '.'), 1)", variables));
  }

  @Test
  public void testBooleanOps() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    assertFalse(runPredicate("not('casey' == foo and true)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("not(not('casey' == foo and true))", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("('casey' == foo) && ( false != true )", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("('casey' == foo) and (FALSE == TRUE)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("'casey' == foo and FALSE", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("'casey' == foo and true", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("true", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("TRUE", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testInCollection() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
    }};
    assertTrue(runPredicate("foo in [ 'casey', 'david' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("foo in [ ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in [ foo, 'david' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in [ 'casey', 'david' ] and 'casey' == foo", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in [ 'casey', 'david' ] and foo == 'casey'", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in [ 'casey' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("foo not in [ 'casey', 'david' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("foo not in [ 'casey', 'david' ] and 'casey' == foo", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("null in [ null, 'something' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("null not in [ null, 'something' ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testInMap() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
    }};
    assertTrue(runPredicate("'casey' in { foo : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("'casey' not in { foo : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in { foo : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("foo not in { foo : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("'foo' in { 'foo' : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("'foo' not in { 'foo' : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("foo in { 'casey' : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("foo not in { 'casey' : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("empty in { foo : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("empty not in { foo : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("'foo' in { }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("null in { 'foo' : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("null not in { 'foo' : 5 }", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test

  public void testShortCircuit_mixedBoolOps() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>();
    assertTrue(runPredicate("(false && true) || true"
            , new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("(false && false) || true"
            , new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("(true || true) && false"
            , new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testInString() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
    }};
    assertTrue(runPredicate("'case' in foo", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("'case' not in foo", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("'case' in empty", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("'case' not in empty", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("'case' in [ foo ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("'case' not in [ foo ]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("null in foo", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("null not in foo", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void inNestedInStatement() throws Exception {
    final Map<String, String> variableMap = new HashMap<>();

    assertTrue(runPredicate("('grok' not in 'foobar') == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'grok' not in ('foobar' == true)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("'grok' in 'grokbar' == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("false in 'grokbar' == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));

    assertTrue(runPredicate("('foo' in 'foobar') == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertFalse(runPredicate("'foo' in ('foobar' == true)", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'grok' not in 'grokbar' == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("false in 'grokbar' == true", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'foo' in ['foo'] AND 'bar' in ['bar']", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("('foo' in ['foo']) AND 'bar' in ['bar']", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("'foo' in ['foo'] AND ('bar' in ['bar'])", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("('foo' in ['foo']) AND ('bar' in ['bar'])", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
    assertTrue(runPredicate("('foo' in ['foo'] AND 'bar' in ['bar'])", new DefaultVariableResolver(variableMap::get,variableMap::containsKey)));
  }

  @Test
  public void testExists() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    assertTrue(runPredicate("exists(foo)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("exists(bar)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("exists(bar) or true", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testMapFunctions_advanced() throws Exception {
    final Map<String, Object> variableMap = new HashMap<String, Object>() {{
      put("foo", "casey");
      put("bar", "bar.casey.grok");
      put("ip", "192.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
      put("myMap", ImmutableMap.of("casey", "apple"));
    }};
    assertTrue(runPredicate("MAP_EXISTS(foo, myMap)", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testLogicalFunctions() throws Exception {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("foo", "casey");
      put("ip", "192.168.0.1");
      put("ip_src_addr", "192.168.0.1");
      put("ip_dst_addr", "10.0.0.1");
      put("other_ip", "10.168.0.1");
      put("empty", "");
      put("spaced", "metron is great");
    }};
    assertTrue(runPredicate("IN_SUBNET(ip, '192.168.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("IN_SUBNET(ip, '192.168.0.0/24', '11.0.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("IN_SUBNET(ip, '192.168.0.0/24', '11.0.0.0/24') in [true]", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("true in IN_SUBNET(ip, '192.168.0.0/24', '11.0.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IN_SUBNET(ip_dst_addr, '192.168.0.0/24', '11.0.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IN_SUBNET(other_ip, '192.168.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    boolean thrown = false;
    try{
      runPredicate("IN_SUBNET(blah, '192.168.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v)));
    }catch(ParseException pe){
      thrown = true;
    }
    assertTrue(thrown);
    assertTrue(runPredicate("true and STARTS_WITH(foo, 'ca')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("true and STARTS_WITH(TO_UPPER(foo), 'CA')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("(true and STARTS_WITH(TO_UPPER(foo), 'CA')) || true", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("true and ENDS_WITH(foo, 'sey')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("not(IN_SUBNET(ip_src_addr, '192.168.0.0/24') and IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("IN_SUBNET(ip_src_addr, '192.168.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("not(IN_SUBNET(ip_src_addr, '192.168.0.0/24'))", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertFalse(runPredicate("IN_SUBNET(ip_dst_addr, '192.168.0.0/24')", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
    assertTrue(runPredicate("not(IN_SUBNET(ip_dst_addr, '192.168.0.0/24'))", new DefaultVariableResolver(v -> variableMap.get(v),v -> variableMap.containsKey(v))));
  }

  @Test
  public void testShortCircuit_conditional() throws Exception {
    assertEquals("foo", run("if true then 'foo' else (if false then 'bar' else 'grok')", new HashMap<>()));
    assertEquals("foo", run("if true_var != null && true_var then 'foo' else (if false then 'bar' else 'grok')", ImmutableMap.of("true_var", true)));
    assertEquals("foo", run("if true then 'foo' else THROW('expression')", new HashMap<>()));
    assertEquals("foo", run("true ? 'foo' : THROW('expression')", new HashMap<>()));
    assertEquals("foo", run("if false then THROW('exception') else 'foo'", new HashMap<>()));
    assertEquals("foo", run("false ? THROW('exception') : 'foo'", new HashMap<>()));
    assertEquals(true, run("RET_TRUE(if true then 'foo' else THROW('expression'))", new HashMap<>()));
    assertEquals("foo", run("if true or (true or THROW('if exception')) then 'foo' else THROW('expression')", new HashMap<>()));
    assertEquals("foo", run("if true or (false or THROW('if exception')) then 'foo' else THROW('expression')", new HashMap<>()));
    assertEquals("foo", run("if NOT(true or (false or THROW('if exception'))) then THROW('expression') else 'foo'", new HashMap<>()));
    assertEquals("foo", run("if NOT('metron' in [ 'metron', 'metronicus'] ) then THROW('expression') else 'foo'", new HashMap<>()));
  }

  @Test
  public void testShortCircuit_nestedIf() throws Exception {
    // Single nested
    // IF true
    //   THEN IF true
    //     THEN 'a'
    //   ELSE 'b'
    // ELSE 'c'
    assertEquals("a", run("IF true THEN IF true THEN 'a' ELSE 'b' ELSE 'c'", new HashMap<>()));
    assertEquals("b", run("IF true THEN IF false THEN 'a' ELSE 'b' ELSE 'c'", new HashMap<>()));
    assertEquals("c", run("IF false THEN IF false THEN 'a' ELSE 'b' ELSE 'c'", new HashMap<>()));

    // 3 layer deep
    // IF true
    //   THEN IF true
    //     THEN IF true
    //       THEN 'a'
    //     ELSE 'b'
    //   ELSE 'c'
    // ELSE 'd'
    assertEquals("a", run("IF true THEN IF true THEN IF true THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("b", run("IF true THEN IF true THEN IF false THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("c", run("IF true THEN IF false THEN IF true THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("c", run("IF true THEN IF false THEN IF false THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF true THEN IF true THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF true THEN IF false THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF false THEN IF true THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF false THEN IF false THEN 'a' ELSE 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));

    // Nested inside inner if
    // IF true
    //   THEN IF true
    //     THEN 'a'
    //   ELSE IF true
    //     THEN 'b'
    //   ELSE 'c'
    // ELSE 'd'
    assertEquals("a", run("IF true THEN IF true THEN 'a' ELSE IF true THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("a", run("IF true THEN IF true THEN 'a' ELSE IF false THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("b", run("IF true THEN IF false THEN 'a' ELSE IF true THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("c", run("IF true THEN IF false THEN 'a' ELSE IF false THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF true THEN 'a' ELSE IF true THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF true THEN 'a' ELSE IF false THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF false THEN 'a' ELSE IF true THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
    assertEquals("d", run("IF false THEN IF false THEN 'a' ELSE IF false THEN 'b' ELSE 'c' ELSE 'd'", new HashMap<>()));
  }

  @Test
  public void testShortCircuit_complexNested() {
    // IF TO_UPPER('foo') == 'FOO'
    //   THEN IF GET_FIRST(MAP(['test_true'], x -> TO_UPPER(x))) == 'TEST_TRUE'
    //     THEN match{ var1 < 12 => 'less', var1 >= 10 => 'more', default => 'default'}
    //   ELSE 'b'
    // ELSE 'c'

    // Should hit the match cases
    assertEquals("less", run("IF TO_UPPER('foo') == 'FOO' THEN IF GET_FIRST(MAP(['test_true'], x -> TO_UPPER(x))) == 'TEST_TRUE' THEN match{ var1 < 10 => 'less', var1 >= 12 => 'more', default => 'default'} ELSE 'b' ELSE 'c'", Collections.singletonMap("var1", 1)));
    assertEquals("default", run("IF TO_UPPER('foo') == 'FOO' THEN IF GET_FIRST(MAP(['test_true'], x -> TO_UPPER(x))) == 'TEST_TRUE' THEN match{ var1 < 10 => 'less', var1 >= 12 => 'more', default => 'default'} ELSE 'b' ELSE 'c'", Collections.singletonMap("var1", 11)));
    assertEquals("more", run("IF TO_UPPER('foo') == 'FOO' THEN IF GET_FIRST(MAP(['test_true'], x -> TO_UPPER(x))) == 'TEST_TRUE' THEN match{ var1 < 10 => 'less', var1 >= 12 => 'more', default => 'default'} ELSE 'b' ELSE 'c'", Collections.singletonMap("var1", 100)));

    // Should fail first if and hit 'c'
    assertEquals("c", run("IF TO_UPPER('bar') == 'FOO' THEN IF GET_FIRST(MAP(['test_true'], x -> TO_UPPER(x))) == 'TEST_TRUE' THEN match{ var1 < 10 => 'less', var1 >= 12 => 'more', default => 'default'} ELSE 'b' ELSE 'c'", Collections.singletonMap("var1", 1)));

    // Should fail second if and hit 'b'
    assertEquals("b", run("IF TO_UPPER('foo') == 'FOO' THEN IF GET_FIRST(MAP(['test_false'], x -> TO_UPPER(x))) == 'TEST_TRUE' THEN match{ var1 < 10 => 'less', var1 >= 12 => 'more', default => 'default'} ELSE 'b' ELSE 'c'", Collections.singletonMap("var1", 1)));
  }

  @Test
  public void testShortCircuit_boolean() throws Exception {
    assertTrue(runPredicate("'metron' in ['metron', 'metronicus', 'mortron'] or (true or THROW('exception'))", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("true or (true or THROW('exception'))", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("true or (false or THROW('exception'))", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("TO_UPPER('foo') == 'FOO' or (true or THROW('exception'))", new DefaultVariableResolver(x -> null,x -> false)));
    assertFalse(runPredicate("false and (true or THROW('exception'))", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("true or false or false or true", new DefaultVariableResolver(x -> null,x -> false)));
    assertFalse(runPredicate("false or (false and THROW('exception'))", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("'casey' == 'casey' or THROW('exception')", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("TO_UPPER('casey') == 'CASEY' or THROW('exception')", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("NOT(TO_UPPER('casey') != 'CASEY') or THROW('exception')", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("(TO_UPPER('casey') == 'CASEY') or THROW('exception')", new DefaultVariableResolver(x -> null,x -> false)));
    assertFalse(runPredicate("NOT(NOT(TO_UPPER('casey') != 'CASEY') or THROW('exception'))", new DefaultVariableResolver(x -> null,x -> false)));
    assertFalse(runPredicate("NOT(NOT(TO_UPPER('casey') != 'CASEY')) and THROW('exception')", new DefaultVariableResolver(x -> null,x -> false)));
    assertTrue(runPredicate("RET_TRUE('foo') or THROW('exception')", new DefaultVariableResolver(x -> null,x -> false)));
    boolean thrown = false;
    try {
      runPredicate("NOT(foo == null or THROW('exception')) and THROW('and exception')",
          new DefaultVariableResolver(x -> null, x -> false));
    }catch(ParseException pe) {
      thrown = true;
    }
    assertTrue(thrown);
    thrown = false;
    try {
      runPredicate("(foo == null or THROW('exception') ) or THROW('and exception')",
          new DefaultVariableResolver(x -> null, x -> false));
    }catch(ParseException pe){
      thrown = true;
    }

    assertTrue(thrown);


    assertTrue(runPredicate("( RET_TRUE('foo', true, false) or ( foo == null or THROW('exception') ) or THROW('and exception')) or THROW('or exception')", new DefaultVariableResolver(x -> null,x -> false)));
  }

  @Test
  public void non_boolean_predicate_throws_exception() {
    final Map<String, String> variableMap = new HashMap<String, String>() {{
      put("protocol", "http");
    }};
    IllegalArgumentException e =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                runPredicate(
                    "TO_UPPER(protocol)",
                    new DefaultVariableResolver(
                        v -> variableMap.get(v), v -> variableMap.containsKey(v))));
    assertEquals("The rule 'TO_UPPER(protocol)' does not return a boolean value.", e.getMessage());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void all_fields_test() {
    final Map<String, Object> varMap1 = new HashMap<String, Object>();
    varMap1.put("field1", "val1");
    final Map<String, Object> varMap2 = new HashMap<String, Object>();
    varMap2.put("field2", "val2");
    VariableResolver resolver = new MapVariableResolver(varMap1, varMap2);
    assertTrue(runPredicate("MAP_GET('field1', _) == 'val1'", resolver));
    assertTrue(runPredicate("MAP_GET('field2', _) == 'val2'", resolver));
    assertTrue(runPredicate("LENGTH(_) == 2", resolver));
    Map<String, Object> ret = (Map<String, Object>) run("_", resolver);
    assertEquals(2, ret.size());
    assertEquals("val1", ret.get("field1"));
    assertEquals("val2", ret.get("field2"));
  }

  @Test
  public void nullAsFalse() {
    checkFalsey("is_alert");
  }

  private void checkFalsey(String falseyExpr) {
    VariableResolver resolver = new MapVariableResolver(new HashMap<>());
    assertTrue(runPredicate(String.format(" %s || true", falseyExpr), resolver));
    assertFalse(runPredicate(String.format("%s && EXCEPTION('blah')", falseyExpr), resolver));
    assertTrue(runPredicate(String.format("NOT(%s)", falseyExpr), resolver));
    assertFalse(runPredicate(String.format("if %s then true else false", falseyExpr), resolver));
    assertFalse(runPredicate(String.format("if %s then true || %s else false", falseyExpr, falseyExpr), resolver));
    assertFalse(runPredicate(String.format("if %s then true || %s else false && %s", falseyExpr, falseyExpr, falseyExpr), resolver));
    assertFalse(runPredicate(String.format("if %s then true || %s else false && (%s || true)", falseyExpr, falseyExpr, falseyExpr), resolver));
    //make sure that nulls aren't replaced by false everywhere, only in boolean expressions.
    assertNull(run(String.format("MAP_GET(%s, {false : 'blah'})", falseyExpr), resolver));
  }

  @Test
  public void emptyAsFalse() {
    checkFalsey("[]");
    checkFalsey("{}");
    checkFalsey("LIST_ADD([])");
  }

}
