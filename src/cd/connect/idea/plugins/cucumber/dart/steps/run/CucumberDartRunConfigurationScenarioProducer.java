package cd.connect.idea.plugins.cucumber.dart.steps.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinScenario;
import org.jetbrains.plugins.cucumber.psi.GherkinScenarioOutline;
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder;

public class CucumberDartRunConfigurationScenarioProducer extends CucumberDartRunConfigurationProducer {
  private static final String SCENARIO_OUTLINE_PARAMETER_REGEXP = "\\\\<.*?\\\\>";
  private static final String ANY_STRING_REGEXP = ".*";
  private static final String NAME_FILTER_TEMPLATE = "^%s$";

  @Override
  protected void setScope(CucumberDartRunnerParameters parameters) {
    parameters.setScope(CucumberDartRunnerParameters.Scope.SCENARIO);
  }

  @Override
  protected String getNameFilter(@NotNull ConfigurationContext context) {
    final PsiElement sourceElement = context.getPsiLocation();

    final GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(sourceElement, GherkinScenario.class, GherkinScenarioOutline.class);
    if (scenario != null) {
      String nameFilter = String.format(NAME_FILTER_TEMPLATE, StringUtil.escapeToRegexp(scenario.getScenarioName()));
      if (scenario instanceof GherkinScenarioOutline) {
        nameFilter = nameFilter.replaceAll(SCENARIO_OUTLINE_PARAMETER_REGEXP, ANY_STRING_REGEXP);
      }

      return nameFilter;
    }

    return "";
  }

  @Override
  protected String getConfigurationName(@NotNull ConfigurationContext context) {
    final PsiElement sourceElement = context.getPsiLocation();
    final GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(sourceElement, GherkinScenario.class, GherkinScenarioOutline.class);

    return "Scenario: " + (scenario != null ? scenario.getScenarioName() : "");
  }
  
  @Nullable
  protected VirtualFile getFileToRun(ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    final GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(element, GherkinScenario.class, GherkinScenarioOutline.class);
    final PsiFile psiFile = scenario != null ? scenario.getContainingFile() : null;
    return psiFile != null ? psiFile.getVirtualFile() : null;
  }
}
