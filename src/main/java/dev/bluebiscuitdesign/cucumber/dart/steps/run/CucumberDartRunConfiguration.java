package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.ExecutionException;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.execution.configurations.RunProfileState;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.jetbrains.lang.dart.ide.runner.base.DartRunConfigurationBase;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import dev.bluebiscuitdesign.cucumber.dart.steps.run.ui.CucumberDartConfigurationEditorForm;

public class CucumberDartRunConfiguration extends DartRunConfigurationBase {
  private CucumberDartRunnerParameters parameters = new CucumberDartRunnerParameters();

  public CucumberDartRunConfiguration(Project project, ConfigurationFactory factory, String name) {
    super(project, factory, name);
  }

  @NotNull
  @Override
  public CucumberDartRunnerParameters getRunnerParameters() {
    return parameters;
  }

  @NotNull
  @Override
  public SettingsEditor<? extends RunConfiguration> getConfigurationEditor() {
    return new CucumberDartConfigurationEditorForm(getProject());
  }

  @Nullable
  @Override
  public String suggestedName() {
    return super.getName();
  }

  @Nullable
  @Override
  public RunProfileState getState(@NotNull Executor executor, @NotNull ExecutionEnvironment executionEnvironment) throws ExecutionException {
    return new CucumberDartRunningTestState(executionEnvironment);
  }

  @Override
  @SuppressWarnings("unchecked")
  public RunConfiguration clone() {
    CucumberDartRunConfiguration c = (CucumberDartRunConfiguration)super.clone();
    c.parameters = parameters.clone();
    return c;
  }
}
