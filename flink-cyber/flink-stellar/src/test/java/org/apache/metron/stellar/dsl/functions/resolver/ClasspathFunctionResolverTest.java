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

package org.apache.metron.stellar.dsl.functions.resolver;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.StellarFunction;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.reflections.util.FilterBuilder;

import java.io.File;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.apache.metron.stellar.dsl.functions.resolver.ClasspathFunctionResolver.Config.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

public class ClasspathFunctionResolverTest {

  private static List<String> expectedFunctions;

  @BeforeAll
  public static void setup() {

    // search the entire classpath for functions - provides a baseline to test against
    Properties config = new Properties();
    // use a permissive regex that should not filter anything
    ClasspathFunctionResolver resolver = create(config);

    expectedFunctions = Lists.newArrayList(resolver.getFunctions());
  }

  /**
   * Create a function resolver to test.
   * @param config The configuration for Stellar.
   */
  public static ClasspathFunctionResolver create(Properties config) {
    ClasspathFunctionResolver resolver = new ClasspathFunctionResolver();

    Context context = new Context.Builder()
            .with(Context.Capabilities.STELLAR_CONFIG, () -> config)
            .build();
    resolver.initialize(context);

    return resolver;
  }

  @Test
  public void testInclude() {

    // setup - include all `org.apache.metron.*` functions
    Properties config = new Properties();
    config.put(STELLAR_SEARCH_INCLUDES_KEY.param(), "org.apache.metron.*");

    // execute
    ClasspathFunctionResolver resolver = create(config);
    List<String> actual = Lists.newArrayList(resolver.getFunctions());

    // validate - should have found all of the functions
    assertEquals(expectedFunctions, actual);
  }

  @Test
  public void testWithMultipleIncludes() {

    // setup - include all of the common and management functions, which is most of them
    Properties config = new Properties();
    config.put(STELLAR_SEARCH_INCLUDES_KEY.param(), "org.apache.metron.common.*, org.apache.metron.management.*");

    // execute
    ClasspathFunctionResolver resolver = create(config);
    List<String> actual = Lists.newArrayList(resolver.getFunctions());

    // validate - should have found all of the functions
    assertTrue(actual.size() > 0);
    assertTrue(actual.size() <= expectedFunctions.size());
  }

  @Test
  public void testExclude() {

    // setup - exclude all `org.apache.metron.*` functions
    Properties config = new Properties();
    config.put(STELLAR_SEARCH_EXCLUDES_KEY.param(), "org.apache.metron.*");

    // use a permissive regex that should not filter anything
    ClasspathFunctionResolver resolver = create(config);
    List<String> actual = Lists.newArrayList(resolver.getFunctions());

    // both should have resolved the same functions
    assertEquals(0, actual.size());
  }

  @Test
  public void testExternalLocal() {
    File jar = new File("src/test/classpath-resources");
    assertTrue(jar.exists());
    Properties config = new Properties();
    config.put(STELLAR_VFS_PATHS.param(), jar.toURI() + "/.*.jar");

    ClasspathFunctionResolver resolver = create(config);
    HashSet<String> functions = new HashSet<>(Lists.newArrayList(resolver.getFunctions()));
    assertTrue(functions.contains("NOW"));
  }

  @Test
  public void testInvalidStellarClass() {
    StellarFunction goodFunc = mock(StellarFunction.class);
    StellarFunction badFunc = mock(StellarFunction.class);
    ClasspathFunctionResolver resolver = new ClasspathFunctionResolver() {
      @Override
      protected Iterable<Class<?>> getStellarClasses(ClassLoader cl) {
        return ImmutableList.of(goodFunc.getClass(), badFunc.getClass());
      }

      @Override
      protected boolean includeClass(Class<?> c, FilterBuilder filterBuilder) {
        if(c != goodFunc.getClass()) {
          throw new LinkageError("failed!");
        }
        return true;
      }
    };
    Set<Class<? extends StellarFunction>> funcs = resolver.resolvables();
    assertEquals(1, funcs.size());
    assertEquals(goodFunc.getClass(), Iterables.getFirst(funcs, null));
  }

}
