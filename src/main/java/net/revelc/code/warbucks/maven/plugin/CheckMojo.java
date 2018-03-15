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

import java.util.List;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

@Mojo(name = "check", defaultPhase = LifecyclePhase.PROCESS_TEST_CLASSES, threadSafe = true,
    requiresDependencyResolution = ResolutionScope.TEST)
public class CheckMojo extends AbstractMojo {

  @Parameter(defaultValue = "${project}", readonly = true)
  MavenProject project;

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
  @Parameter(alias = "ignoreRuleFailures", property = "warbucks.ignoreRuleFailures",
      defaultValue = "false")
  boolean ignoreRuleFailures;

  /**
   * Causes the plugin to process all rules, rather than failing after the first rule failure.
   *
   * @since 1.0.0
   */
  @Parameter(alias = "processAllRules", property = "warbucks.processAllRules",
      defaultValue = "true")
  private boolean processAllRules;

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

    long totalFailures = 0;
    for (int i = 0; i < rules.size(); i++) {
      totalFailures += new RuleProcessor(this, rules.get(i), i).process();
      if (totalFailures > 0 && !processAllRules) {
        getLog().debug("Skipping additional rules, if any, due to processAllRules=false");
        break;
      }
    }
    getLog().info("Total class failures: " + totalFailures);
    if (totalFailures > 0 && !ignoreRuleFailures) {
      throw new MojoFailureException("There were rule failures. See the output above for details.");
    }
  }

}
