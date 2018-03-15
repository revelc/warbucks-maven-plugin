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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Test;
import org.mockito.Mockito;

public class TestRuleProcessor {

  @Test
  public void testValidModifier() {
    List<Class<?>> allClasses = Arrays.asList(PublicClass.class, PackagePrivateClass.class,
        ProtectedClass.class, PrivateClass.class);
    int testCount = 0;
    for (EnumSet<Modifier> modifiers : combineModifiers()) {
      assertFalse(modifiers.isEmpty());
      ++testCount;
      Rule rule = createRule(modifiers);
      List<Class<?>> valid = allClasses.stream()
          .filter(clz -> RuleProcessor.isValidModifier(clz, rule)).collect(Collectors.toList());
      assertEquals(rule.getIncludePublicClasses(), valid.contains(PublicClass.class));
      assertEquals(rule.getIncludePackagePrivateClasses(),
          valid.contains(PackagePrivateClass.class));
      assertEquals(rule.getIncludeProtectedClasses(), valid.contains(ProtectedClass.class));
      assertEquals(rule.getIncludePrivateClasses(), valid.contains(PrivateClass.class));
    }
    assertTrue(testCount != 0);
  }

  private static List<EnumSet<Modifier>> combineModifiers() {
    List<EnumSet<Modifier>> single =
        Arrays.asList(EnumSet.of(Modifier.PUBLIC), EnumSet.of(Modifier.PACKAGE_PRIVATE),
            EnumSet.of(Modifier.PROTECTED), EnumSet.of(Modifier.PRIVATE));
    List<EnumSet<Modifier>> all = new ArrayList<>(single);
    List<EnumSet<Modifier>> last = single;
    for (int i = 1; i < Modifier.values().length; ++i) {
      last = merge(single, last, i + 1);
      all.addAll(last);
    }
    return all;
  }

  private static List<EnumSet<Modifier>> merge(List<EnumSet<Modifier>> lhs,
      List<EnumSet<Modifier>> rhs, int expectedSize) {
    List<EnumSet<Modifier>> merged = new ArrayList<>();
    for (EnumSet<Modifier> s0 : lhs) {
      for (EnumSet<Modifier> s1 : rhs) {
        if (s0.equals(s1)) {
          continue;
        }
        EnumSet<Modifier> merge = EnumSet.copyOf(s0);
        merge.addAll(s1);
        if (merge.size() == expectedSize && !merged.contains(merge)) {
          merged.add(merge);
        }
      }
    }
    return merged;
  }

  /**
   * @return a mock Rule having passed modifiers
   */
  private static Rule createRule(EnumSet<Modifier> modifiers) {
    Rule rule = Mockito.mock(Rule.class);
    for (Modifier mod : Modifier.values()) {
      boolean value = modifiers.contains(mod);
      switch (mod) {
        case PUBLIC:
          Mockito.when(rule.getIncludePublicClasses()).thenReturn(value);
          break;
        case PACKAGE_PRIVATE:
          Mockito.when(rule.getIncludePackagePrivateClasses()).thenReturn(value);
          break;
        case PROTECTED:
          Mockito.when(rule.getIncludeProtectedClasses()).thenReturn(value);
          break;
        case PRIVATE:
          Mockito.when(rule.getIncludePrivateClasses()).thenReturn(value);
          break;
        default:
          throw new RuntimeException("Unsupported modififer:" + mod);
      }
    }
    return rule;
  }

  enum Modifier {
    PUBLIC, PACKAGE_PRIVATE, PROTECTED, PRIVATE
  }

  public static class PublicClass {
  }

  static class PackagePrivateClass {
  }

  protected static class ProtectedClass {
  }

  private static class PrivateClass {
  }
}
