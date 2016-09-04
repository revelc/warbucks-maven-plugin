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

import java.io.File;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collection;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import com.google.common.reflect.ClassPath;
import com.google.common.reflect.ClassPath.ClassInfo;

@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true, requiresDependencyResolution = ResolutionScope.TEST)
public class CheckMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  private MavenProject project;

  @Parameter(defaultValue = "${plugin}", readonly = true)
  private PluginDescriptor plugin;

  /**
   * Allows skipping execution of this plugin.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "skip", property = "warbucks.skip", defaultValue = "false")
  private boolean skip;

  /**
   * Allows ignoring rule failures, so they don't result in a build failure.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "ignoreRuleFailures", property = "warbucks.ignoreRuleFailures", defaultValue = "false")
  private boolean ignoreRuleFailures;

  /**
   * The rules for this plugin to check.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "rules", required = true)
  private List<Rule> rules;

  @Override
  public void execute() throws MojoFailureException, MojoExecutionException {
    if (skip) {
      getLog().info("Skipping execution of warbucks-maven-plugin");
      return;
    }

    if (rules == null || rules.isEmpty()) {
      getLog().info("No rules to process");
      return;
    }

    int ruleNumber = 1;
    for (Rule r : rules) {
      getLog().debug("Processing rule number " + ruleNumber);
      processRule(r, ruleNumber);
      ruleNumber++;
    }
  }

  private void processRule(Rule r, int ruleNumber) throws MojoExecutionException, MojoFailureException {
    Pattern classPattern = Pattern.compile(r.getClassPattern());
    Pattern classAnnotationPattern = Pattern.compile(r.getClassAnnotationPattern());
    try (URLClassLoader cl = new URLClassLoader(getURLs(r.getIncludeTests() ? project.getTestClasspathElements() : project.getCompileClasspathElements()))) {
      List<ClassInfo> failedClasses = ClassPath.from(cl).getAllClasses().stream().filter(x -> classPattern.matcher(x.getName()).matches()).filter(x -> {
        boolean foundMatch = false;
        for (Annotation s : x.load().getAnnotations()) {
          getLog().debug("Found annotation class '" + s.annotationType().getName() + "' on class '" + x.getName() + "'");
          if (classAnnotationPattern.matcher(s.annotationType().getName()).matches()) {
            foundMatch = true;
            break;
          }
        }
        return !foundMatch; // filter non-matching classes
      }).collect(Collectors.toList());
      if (failedClasses.isEmpty()) {
        getLog().debug("All matching classes for rule " + ruleNumber + " were annotated with a matching annotation");
        return;
      }

      String failMessage = "Rule " + ruleNumber + ": Class '%s' did not have an annotation matching the pattern '" + r.getClassAnnotationPattern() + "'";
      if (ignoreRuleFailures) {
        failedClasses.forEach(x -> getLog().warn(String.format(failMessage, x.getName())));
      } else {
        failedClasses.forEach(x -> getLog().error(String.format(failMessage, x.getName())));
        throw new MojoFailureException("Some rules failed. See above output for details.");
      }
    } catch (IOException | DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Problem loading classes", e);
    }
  }

  private URL[] getURLs(Collection<String> classPathElements) {
    return classPathElements.stream().distinct().map(element -> {
      try {
        return new File(element).toURI().toURL();
      } catch (MalformedURLException e) {
        throw new AssertionError("Could not convert class path element to URL", e);
      }
    }).toArray(URL[]::new);
  }

}
