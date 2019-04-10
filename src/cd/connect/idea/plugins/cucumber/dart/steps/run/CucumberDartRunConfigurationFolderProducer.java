package cd.connect.idea.plugins.cucumber.dart.steps.run;

import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
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
  protected VirtualFile getFileToRun(ConfigurationContext context) {
    final PsiElement element = context.getPsiLocation();
    if (element instanceof PsiDirectory) {
      return ((PsiDirectory) element).getVirtualFile();
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
