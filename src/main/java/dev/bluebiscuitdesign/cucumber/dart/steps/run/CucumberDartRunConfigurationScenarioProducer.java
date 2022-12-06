package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinScenario;
import org.jetbrains.plugins.cucumber.psi.GherkinScenarioOutline;
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder;

public class CucumberDartRunConfigurationScenarioProducer extends CucumberDartRunConfigurationProducer {

  @Override
  protected void setScope(CucumberDartRunnerParameters parameters) {
    parameters.setCucumberScope(CucumberDartRunnerParameters.Scope.SCENARIO);
  }

  @Override
  protected String getNameFilter(@NotNull ConfigurationContext context) {
    final PsiElement sourceElement = context.getPsiLocation();

    final GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(sourceElement, GherkinScenario.class, GherkinScenarioOutline.class);
    return scenario.getScenarioName();
  }

  @Override
  protected String getConfigurationName(@NotNull ConfigurationContext context) {
    final PsiElement sourceElement = context.getPsiLocation();
    final GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(sourceElement, GherkinScenario.class, GherkinScenarioOutline.class);

    return "Ogurets: " + (scenario != null ? scenario.getScenarioName() : "");
  }

  @Nullable
  protected PsiFile getPsiFileToRun(ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    final GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(element, GherkinScenario.class, GherkinScenarioOutline.class);
    return scenario != null ? scenario.getContainingFile() : null;

  }
}
