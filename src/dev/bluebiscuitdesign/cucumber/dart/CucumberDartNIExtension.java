package dev.bluebiscuitdesign.cucumber.dart;

import dev.bluebiscuitdesign.cucumber.dart.steps.DartAnnotatedStepDefinition;
import dev.bluebiscuitdesign.cucumber.dart.steps.DartStepDefinitionCreator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleUtilCore;
import com.intellij.openapi.progress.ProgressManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.indexing.FileBasedIndex;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.psi.DartFile;
import com.jetbrains.lang.dart.psi.DartMethodDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.BDDFrameworkType;
import org.jetbrains.plugins.cucumber.StepDefinitionCreator;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.*;

public class CucumberDartNIExtension extends AbstractCucumberExtension {
  @Override
  public boolean isStepLikeFile(@NotNull PsiElement child, @NotNull PsiElement parent) {
    return child instanceof DartFile && ((DartFile)child).getName().endsWith(".dart");
  }

  @Override
  public boolean isWritableStepLikeFile(@NotNull PsiElement child, @NotNull PsiElement parent) {
    return isStepLikeFile(child, parent);
  }

  @NotNull
  @Override
  public BDDFrameworkType getStepFileType() {
    return new BDDFrameworkType(DartFileType.INSTANCE, "Dart 2");
  }

  @NotNull
  @Override
  public StepDefinitionCreator getStepDefinitionCreator() {
    return new DartStepDefinitionCreator();
  }

  @Override
  public List<AbstractStepDefinition> loadStepsFor(@Nullable PsiFile featureFile, @NotNull Module module) {
    final List<AbstractStepDefinition> result = new ArrayList<>();
    final FileBasedIndex fileBasedIndex = FileBasedIndex.getInstance();

    Project project = module.getProject();
    fileBasedIndex.processValues(DartCucumberIndex.INDEX_ID, true, null,
            (file, value) -> {
              ProgressManager.checkCanceled();

              PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
              if (psiFile == null) {
                return true;
              }

              for (Integer offset : value) {
                PsiElement element = psiFile.findElementAt(offset + 1);
                DartMethodDeclaration member = PsiTreeUtil.getParentOfType(element, DartMethodDeclaration.class);
                if (member == null) {
                  continue;
                }
                String annotation = CucumberDartUtil.findDartCucumberAnnotation(member);
                if (annotation != null) {
                  result.add(new DartAnnotatedStepDefinition(member, annotation));
                }
              }
              return true;
            }, GlobalSearchScope.projectScope(project));
    return result;
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
