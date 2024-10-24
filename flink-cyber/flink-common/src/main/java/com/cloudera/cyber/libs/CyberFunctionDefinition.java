/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.libs;

import com.cloudera.cyber.CyberFunction;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.atteo.classindex.ClassIndex;

@Getter
@Slf4j
public class CyberFunctionDefinition {

    private final boolean isValid;
    private final String functionName;
    private final List<Parameter> parameters;
    private final Class<?> implementationClass;

    private static final List<CyberFunctionDefinition> CYBER_FUNCTION_DEFINITIONS =
          StreamSupport.stream(ClassIndex.getAnnotated(CyberFunction.class).spliterator(), false)
                .map(funcClass -> new CyberFunctionDefinition(funcClass.getAnnotation(CyberFunction.class).value(),
                      funcClass))
                .filter(f -> f.isValid).collect(Collectors.toList());

    public static Stream<CyberFunctionDefinition> findAll() {
        return CYBER_FUNCTION_DEFINITIONS.stream();
    }

    public CyberFunctionDefinition(String functionName, Class<?> implementationClass) {
        this.implementationClass = implementationClass;
        this.functionName = functionName;
        List<Parameter> parameters = Collections.emptyList();
        boolean isValid = false;
        if (StringUtils.isNotBlank(functionName)) {
            Optional<Method> evalMethod =
                  Stream.of(implementationClass.getDeclaredMethods()).filter(m -> m.getName().equals("eval"))
                        .findFirst();
            if (evalMethod.isPresent()) {
                parameters = Stream.of(evalMethod.get().getParameters()).collect(Collectors.toList());
                isValid = true;
            } else {
                log.error(
                      "Cyber Function defined in class {} does not have an eval method.  Function {} will not be defined in javascript.",
                      implementationClass.getCanonicalName(), functionName);
            }
        } else {
            log.error("Cyber Function defined in class {} has a blank function name.",
                  implementationClass.getCanonicalName());
        }
        this.isValid = isValid;
        this.parameters = parameters;
    }

}
