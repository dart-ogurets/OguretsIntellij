package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFileSystemItem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.CucumberBundle;

public class CucumberDartRunConfigurationFolderProducer extends CucumberDartRunConfigurationProducer {
  @Override
  protected String getConfigurationName(@NotNull ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    return CucumberBundle.message("cucumber.run.all.features", ((PsiDirectory)element).getVirtualFile().getName());
  }

  @Nullable
  @Override
  protected PsiFileSystemItem getPsiFileToRun(ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    if (element instanceof PsiDirectory) {
      return ((PsiDirectory) element);
    }
    return null;
  }

  @Override
  protected void setScope(CucumberDartRunnerParameters parameters) {
    parameters.setCucumberScope(CucumberDartRunnerParameters.Scope.FOLDER);
  }

  @Override
  protected String getNameFilter(@NotNull ConfigurationContext context) {
    return "";
  }
}
