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

package org.apache.metron.stellar.dsl.functions;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.apache.metron.stellar.common.utils.StellarProcessorUtils.run;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataStructureFunctionsTest {

  @Test
  public void is_empty_handles_happy_path() {
    DataStructureFunctions.IsEmpty isEmpty = new DataStructureFunctions.IsEmpty();
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of("hello"));
      assertThat("should be false", empty, CoreMatchers.equalTo(false));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(ImmutableList.of("hello", "world")));
      assertThat("should be false", empty, CoreMatchers.equalTo(false));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(1));
      assertThat("should be false", empty, CoreMatchers.equalTo(false));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(ImmutableMap.of("mykey", "myvalue")));
      assertThat("should be false", empty, CoreMatchers.equalTo(false));
    }
  }

  @Test
  public void is_empty_handles_empty_values() {
    DataStructureFunctions.IsEmpty isEmpty = new DataStructureFunctions.IsEmpty();
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of());
      assertThat("should be true", empty, CoreMatchers.equalTo(true));
    }
    {
      boolean empty = (boolean) isEmpty.apply(null);
      assertThat("should be true", empty, CoreMatchers.equalTo(true));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(""));
      assertThat("should be true", empty, CoreMatchers.equalTo(true));
    }
    {
      boolean empty = (boolean) isEmpty.apply(ImmutableList.of(ImmutableMap.of()));
      assertThat("should be true", empty, CoreMatchers.equalTo(true));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void listAdd_number() {
    for(String expr : ImmutableList.of("LIST_ADD(my_list, 1)"
                                      ,"LIST_ADD([], 1)"
                                      ,"LIST_ADD([], val)"
                                      )
       )
    {
      Object o = run(expr, ImmutableMap.of("my_list", new ArrayList<>(), "val", 1));
      assertTrue(o instanceof List);
      List<Number> result = (List<Number>) o;
      assertEquals(1, result.size());
      assertEquals(1, result.get(0));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void listAdd_mixed() {
    for(String expr : ImmutableList.of("LIST_ADD(my_list, 1)"
                                      ,"LIST_ADD(['foo'], 1)"
                                      ,"LIST_ADD(['foo'], val)"
                                      )
       )
    {
      ArrayList<Object> list = new ArrayList<>();
      list.add("foo");
      Object o = run(expr, ImmutableMap.of("my_list", list, "val", 1));
      assertTrue(o instanceof List);
      List<Object> result = (List<Object>) o;
      assertEquals(2, result.size());
      assertEquals("foo", result.get(0));
      assertEquals(1, result.get(1));
    }
  }

  @Test
  @SuppressWarnings("unchecked")
  public void listAdd_number_nonempty() {
    for(String expr : ImmutableList.of("LIST_ADD(my_list, 2)"
                                      ,"LIST_ADD([1], 2)"
                                      ,"LIST_ADD([1], val)"
                                      )
       )
    {
      ArrayList<Integer> list = new ArrayList<>();
      list.add(1);
      Object o = run(expr, ImmutableMap.of("my_list", list, "val", 2));
      assertTrue(o instanceof List);
      List<Number> result = (List<Number>) o;
      assertEquals(2, result.size());
      assertEquals(1, result.get(0));
      assertEquals(2, result.get(1));
    }
  }
}
