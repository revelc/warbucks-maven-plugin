/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.revelc.code.warbucks.maven.plugin;

import java.util.Arrays;

public class Rule {

  private String scope;
  private String classPattern;
  private String classAnnotationPattern;

  public Scope getScope() {
    if (scope == null || scope.isEmpty()) {
      return Scope.BOTH;
    }
    try {
      return Scope.valueOf(scope.toUpperCase());
    } catch (IllegalArgumentException e) {
      throw new IllegalArgumentException("Unrecognized scope '" + scope + "' declared for rule. Scope must be one of " + Arrays.toString(Scope.values()) + ".");
    }
  }

  public String getClassPattern() {
    return classPattern;
  }

  public String getClassAnnotationPattern() {
    return classAnnotationPattern;
  }

}
