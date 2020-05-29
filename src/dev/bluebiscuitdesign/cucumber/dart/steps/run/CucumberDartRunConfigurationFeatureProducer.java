package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinFeature;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.psi.GherkinScenario;
import org.jetbrains.plugins.cucumber.psi.GherkinScenarioOutline;
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder;

public class CucumberDartRunConfigurationFeatureProducer extends CucumberDartRunConfigurationProducer {
  @Override
  protected String getConfigurationName(@NotNull ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    final GherkinFeature feature = PsiTreeUtil.getParentOfType(element, GherkinFeature.class);

    if (feature != null && feature.getFeatureName() != null && feature.getFeatureName().trim().length() > 0) {
      return "Ogurets: " + feature.getFeatureName().trim();
    }

    final VirtualFile featureFile = getFileToRun(context);
    
    assert featureFile != null;
    return "Ogurets: " + featureFile.getNameWithoutExtension();
  }

  @Nullable
  @Override
  protected PsiFileSystemItem getPsiFileToRun(ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    final GherkinStepsHolder scenario = PsiTreeUtil.getParentOfType(element, GherkinScenario.class, GherkinScenarioOutline.class);

    if (scenario != null) {
      return null;
    }

    if (element != null && element.getContainingFile() instanceof GherkinFile) {
      return element.getContainingFile();
    }

    return null;
  }

  @Override
  protected void setScope(CucumberDartRunnerParameters parameters) {
    parameters.setCucumberScope(CucumberDartRunnerParameters.Scope.FEATURE);
  }

  @Override
  protected String getNameFilter(@NotNull ConfigurationContext context) {
    return "";
  }
}
