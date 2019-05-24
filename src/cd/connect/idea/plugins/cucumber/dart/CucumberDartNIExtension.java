package cd.connect.idea.plugins.cucumber.dart;

import cd.connect.idea.plugins.cucumber.dart.steps.DartStepDefinitionCreator;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.ModuleManager;
import com.intellij.openapi.module.ModuleType;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.ModuleRootManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.projectWizard.DartModuleBuilder;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartClassMembers;
import com.jetbrains.lang.dart.psi.DartFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.BDDFrameworkType;
import org.jetbrains.plugins.cucumber.StepDefinitionCreator;
import cd.connect.idea.plugins.cucumber.dart.steps.DartAnnotatedStepDefinition;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;
import org.jetbrains.plugins.cucumber.steps.NotIndexedCucumberExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CucumberDartNIExtension extends NotIndexedCucumberExtension {



  @Override
  protected void loadStepDefinitionRootsFromLibraries(Module module, List<PsiDirectory> roots, Set<String> directories) {
  }

  @Override
  protected Collection<AbstractStepDefinition> getStepDefinitions(@NotNull PsiFile psiFile) {
    final List<AbstractStepDefinition> newDefs = new ArrayList<>();
    if (psiFile instanceof DartFile) {
      final DartClassDefinition clazz = PsiTreeUtil.getChildOfType(psiFile, DartClassDefinition.class);

      if (clazz != null && clazz.getClassBody() != null && clazz.getClassBody().getClassMembers() != null) {
        DartClassMembers cMembers = clazz.getClassBody().getClassMembers();
        cMembers.getMethodDeclarationList().forEach(member -> {
          String annotation = CucumberDartUtil.findDartCucumberAnnotation(member);

          if (annotation != null) {
            newDefs.add(new DartAnnotatedStepDefinition(member, annotation));
          }
        });
      }
    }
    return newDefs;
  }

  @Override
  protected void collectAllStepDefsProviders(@NotNull List<VirtualFile> providers, @NotNull Project project) {
    final Module[] modules = ModuleManager.getInstance(project).getModules();
    final DartModuleBuilder dmb = new DartModuleBuilder();
    ModuleType moduleType = dmb.getModuleType();
    for (Module module : modules) {
      if (ModuleType.get(module).equals(moduleType)) {
        final VirtualFile[] roots = ModuleRootManager.getInstance(module).getContentRoots();
        ContainerUtil.addAll(providers, roots);
      }
    }

  }

  @Override
  public void findRelatedStepDefsRoots(@NotNull final Module module, @NotNull final PsiFile featureFile,
                                       List<PsiDirectory> newStepDefinitionsRoots, Set<String> processedStepDirectories) {
    // ToDo: check if inside test folder
    PsiDirectory currDir = featureFile.getContainingDirectory();
    PsiDirectory featureFileDirectory = currDir;

    while (currDir != null && currDir.isDirectory() && (!currDir.getName().equals("test_driver") || currDir.getName().equals("test"))) {
      if ("steps".equals(currDir.getName())) {
        if (!processedStepDirectories.contains(currDir.getVirtualFile().getPath())) {
          newStepDefinitionsRoots.add(currDir);
        }
      }

      Arrays.stream(currDir.getSubdirectories()).filter(d -> "steps".equals(d.getName())).findFirst().ifPresent(d -> {
        if (!processedStepDirectories.contains(d.getVirtualFile().getPath())) {
          newStepDefinitionsRoots.add(d);
        }
      });

      currDir = currDir.getParentDirectory();
    }

    if (currDir != null && !processedStepDirectories.contains(currDir.getVirtualFile().getPath())) {
      newStepDefinitionsRoots.add(currDir);
    }
  }

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

  @NotNull
  @Override
  public Collection<String> getGlues(@NotNull GherkinFile file, Set<String> gluesFromOtherFiles) {
    return gluesFromOtherFiles == null ? new HashSet<>() : gluesFromOtherFiles;
  }
}
