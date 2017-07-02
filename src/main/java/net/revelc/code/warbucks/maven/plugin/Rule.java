/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
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

public class Rule {

  private String classAnnotationPattern;
  private String classPattern;
  private boolean includeMainClasses = true;
  private boolean includeTestClasses = false;

  public String getClassAnnotationPattern() {
    return classAnnotationPattern;
  }

  public String getClassPattern() {
    return classPattern;
  }

  public boolean getIncludeMainClasses() {
    return includeMainClasses;
  }

  public boolean getIncludeTestClasses() {
    return includeTestClasses;
  }

}
