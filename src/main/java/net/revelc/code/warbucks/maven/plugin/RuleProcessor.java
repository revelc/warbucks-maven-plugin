/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.revelc.code.warbucks.maven.plugin;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;
import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.logging.Log;

class RuleProcessor {

  private final Rule rule;
  private final String logPrefix;
  private final Log log;
  private final CheckMojo mojo;

  RuleProcessor(CheckMojo mojo, Rule rule, int n) {
    this.mojo = mojo;
    this.rule = rule;

    this.log = mojo.getLog();
    this.logPrefix = "Rule " + n + ": ";
  }

  private void debug(String s) {
    log.debug(logPrefix + s);
  }

  private void error(String s) {
    log.error(logPrefix + s);
  }

  private void warn(String s) {
    log.warn(logPrefix + s);
  }

  private void info(String s) {
    log.info(logPrefix + s);
  }

  // returns the number of rule failures
  long process() throws MojoExecutionException {
    debug("Begin processing");
    try (URLClassLoader cl = getClassLoader()) {
      AtomicLong matches = new AtomicLong(0);
      long failures = ClassPath.from(cl).getAllClasses().stream().filter(isProjectClass())
          .filter(matchesClassPattern()).filter(isValidModifier())
          .peek(x -> matches.incrementAndGet()).filter(hasRequiredAnnotation().negate()).count();
      info("Class Matches: " + matches.get());
      info("Class Failures: " + failures);
      return failures;
    } catch (IOException | DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Problem loading classes", e);
    }
  }

  private URLClassLoader getClassLoader() throws DependencyResolutionRequiredException {
    URL[] urls = getURLs(mojo.project.getTestClasspathElements());
    // best practice is to create class loaders in doPrivileged blocks; caught by findbugs
    return AccessController
        .doPrivileged((PrivilegedAction<URLClassLoader>) (() -> new URLClassLoader(urls)));
  }

  // verifies that the class is part of this project, and not included in the classloader as a
  // dependency
  private Predicate<ClassInfo> isProjectClass() {
    String mainDir = mojo.project.getBuild().getOutputDirectory();
    String testDir = mojo.project.getBuild().getTestOutputDirectory();
    return x -> {
      boolean foundMainClass =
          rule.getIncludeMainClasses() && new File(mainDir, x.getResourceName()).exists();
      boolean foundTestClass = !foundMainClass && rule.getIncludeTestClasses()
          && new File(testDir, x.getResourceName()).exists();
      if (foundMainClass) {
        debug("Found '" + x.getName() + "' in " + mainDir);
      } else if (foundTestClass) {
        debug("Found '" + x.getName() + "' in " + testDir);
      }
      return foundMainClass || foundTestClass;
    };
  }

  private Predicate<ClassInfo> isValidModifier() {
    return classInfo -> rule.getIncludePublicClasses() && rule.getIncludeProtectedClasses()
        && rule.getIncludePackagePrivateClasses() && rule.getIncludePrivateClasses() ? true
            : isValidModifier(classInfo.load(), rule);
  }

  @VisibleForTesting
  static boolean isValidModifier(Class<?> clz, Rule rule) {
    int mod = clz.getModifiers();
    if (Modifier.isPublic(mod)) {
      return rule.getIncludePublicClasses();
    } else if (Modifier.isProtected(mod)) {
      return rule.getIncludeProtectedClasses();
    } else if (Modifier.isPrivate(mod)) {
      return rule.getIncludePrivateClasses();
    } else {
      return rule.getIncludePackagePrivateClasses();
    }
  }

  // only check classes which match the specified pattern
  private Predicate<ClassInfo> matchesClassPattern() {
    Pattern classPattern = Pattern.compile(rule.getClassPattern());
    return x -> classPattern.matcher(x.getName()).matches();
  }

  // check class for required annotations
  private Predicate<ClassInfo> hasRequiredAnnotation() {
    Pattern classAnnotationPattern = Pattern.compile(rule.getClassAnnotationPattern());
    return x -> {
      boolean foundMatch = false;
      for (Annotation s : x.load().getAnnotations()) {
        debug("Found annotation class '" + s.annotationType().getName() + "' on class '"
            + x.getName() + "'");
        if (classAnnotationPattern.matcher(s.annotationType().getName()).matches()) {
          foundMatch = true;
          break;
        }
      }
      if (!foundMatch) {
        String failMessage =
            String.format("Class '%s' did not have an annotation matching the pattern '%s'",
                x.getName(), rule.getClassAnnotationPattern());
        if (mojo.ignoreRuleFailures) {
          warn(failMessage);
        } else {
          error(failMessage);
        }
      }
      return foundMatch;
    };
  }

  private static URL[] getURLs(Collection<String> classPathElements) {
    return classPathElements.stream().distinct().map(element -> {
      try {
        return new File(element).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new AssertionError("Could not convert class path element to URL", e);
      }
    }).toArray(URL[]::new);
  }

}
