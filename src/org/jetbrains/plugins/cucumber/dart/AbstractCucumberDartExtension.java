package org.jetbrains.plugins.cucumber.dart;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.roots.ProjectRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.lang.dart.psi.DartFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.psi.GherkinRecursiveElementVisitor;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.*;

public abstract class AbstractCucumberDartExtension extends AbstractCucumberExtension {
  @Override
  public boolean isStepLikeFile(@NotNull final PsiElement child, @NotNull final PsiElement parent) {
    return child instanceof DartFile && ((DartFile)child).getName().endsWith(".dart");
  }

  @Override
  public boolean isWritableStepLikeFile(@NotNull PsiElement child, @NotNull PsiElement parent) {
    return isStepLikeFile(child, parent);
  }

  @NotNull
  @Override
  public Collection<String> getGlues(@NotNull GherkinFile file, Set<String> gluesFromOtherFiles) {
    return gluesFromOtherFiles == null ? new HashSet<>() : gluesFromOtherFiles;
  }



  @Override
  public Collection<? extends PsiFile> getStepDefinitionContainers(@NotNull GherkinFile featureFile) {
    final Module module = ModuleUtilCore.findModuleForPsiElement(featureFile);
    if (module == null) {
      return Collections.emptySet();
    }
    List<AbstractStepDefinition> stepDefs = loadStepsFor(featureFile, module);

    Set<PsiFile> result = new HashSet<>();
    for (AbstractStepDefinition stepDef : stepDefs) {
      PsiElement stepDefElement = stepDef.getElement();
      if (stepDefElement != null) {
        final PsiFile psiFile = stepDefElement.getContainingFile();
        PsiDirectory psiDirectory = psiFile.getParent();
        if (psiDirectory != null && isWritableStepLikeFile(psiFile, psiDirectory)) {
          result.add(psiFile);
        }
      }
    }
    return result;
  }
}
