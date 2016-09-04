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
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

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

    for (Rule r : rules) {
      processRule(r);
    }
  }

  private void processRule(Rule r) throws MojoExecutionException {
    Set<String> classPathElements = new HashSet<>();
    Scope scope = r.getScope();
    try {
      Stream.concat(project.getCompileClasspathElements().stream(), project.getRuntimeClasspathElements().stream());
      switch (scope) {
        case COMPILE:
          classPathElements.addAll(project.getCompileClasspathElements());
          break;
        case TEST:
          classPathElements.addAll(project.getTestClasspathElements());
          classPathElements.removeAll(project.getCompileClasspathElements());
          break;
        case BOTH:
          classPathElements.addAll(project.getCompileClasspathElements());
          classPathElements.addAll(project.getTestClasspathElements());
          break;
        default:
          throw new MojoExecutionException("Unknown scope: " + scope);
      }
    } catch (DependencyResolutionRequiredException e) {
      throw new MojoExecutionException("Could not resolve artifacts", e);
    }

    try (URLClassLoader cl = new URLClassLoader(getURLs(classPathElements))) {
      ClassPath cp = ClassPath.from(cl);
      cp.getAllClasses().forEach(x -> System.out.println("blahblah: " + x));
    } catch (IOException e) {
      throw new MojoExecutionException("Problem loading classes", e);
    }
  }

  private URL[] getURLs(Set<String> classPathElements) throws MojoExecutionException {
    try {
      return classPathElements.stream().map(element -> {
        try {
          return new File(element).toURI().toURL();
        } catch (MalformedURLException e) {
          throw new RuntimeException(e);
        }
      }).toArray(URL[]::new);
    } catch (RuntimeException e) {
      if (e.getCause() instanceof MalformedURLException) {
        throw new MojoExecutionException("Problem loading classes", e.getCause());
      } else {
        throw e;
      }
    }
  }

}
