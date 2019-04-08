package org.jetbrains.plugins.cucumber.dart.steps.run;

import com.intellij.execution.JavaExecutionUtil;
import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.ide.runner.util.TestUtil;
import com.jetbrains.lang.dart.psi.DartCallExpression;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartFile;
import com.jetbrains.lang.dart.util.DartResolveUtil;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import com.jetbrains.lang.dart.util.PubspecYamlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;
import org.jetbrains.plugins.cucumber.psi.GherkinScenario;
import org.jetbrains.plugins.cucumber.psi.GherkinScenarioOutline;
import org.jetbrains.plugins.cucumber.psi.GherkinStepsHolder;

import java.util.List;

abstract public class CucumberDartRunConfigurationProducer extends RunConfigurationProducer<CucumberDartRunConfiguration> {
  public CucumberDartRunConfigurationProducer() {
    super(CucumberDartRunConfigurationType.class);
  }

  @Override
  protected boolean setupConfigurationFromContext(final @NotNull CucumberDartRunConfiguration configuration,
                                                  final @NotNull ConfigurationContext context,
                                                  final @NotNull Ref<PsiElement> sourceElement) {
    final boolean ok;

    final PsiElement location = context.getPsiLocation();
    if (location instanceof PsiDirectory) {
      ok = setupRunnerParametersForFolderIfApplicable(configuration.getProject(), configuration.getRunnerParameters(),
        ((PsiDirectory)location).getVirtualFile());
    }
    else {
      ok = setupRunnerParametersForFileIfApplicable(configuration.getRunnerParameters(), context, sourceElement);
    }

    if (ok) {
      configuration.setName(getConfigurationName(context));
      configuration.setGeneratedName();
    }

    return ok;
  }

  protected abstract String getConfigurationName(final @NotNull ConfigurationContext context);

  @Override
  public boolean isConfigurationFromContext(CucumberDartRunConfiguration runConfiguration, ConfigurationContext context) {
    Location location = context.getLocation();
    if (location == null) {
      return false;
    }

    final VirtualFile fileToRun = getFileToRun(context);
    if (fileToRun == null) {
      return false;
    }

    if (!fileToRun.getPath().equals(runConfiguration.getRunnerParameters().getCucumberFilePath())) {
      return false;
    }

    if (!Comparing.strEqual(getNameFilter(context), runConfiguration.getRunnerParameters().getNameFilter())) {
      return false;
    }

    return true;
  }

  @Nullable
  protected abstract VirtualFile getFileToRun(ConfigurationContext context);




  // ensure the dart test package + dherkin2 is available to us
  private boolean setupRunnerParametersForFolderIfApplicable(@NotNull final Project project,
                                                                    @NotNull final CucumberDartRunnerParameters params,
                                                                    @NotNull final VirtualFile dir) {

    if (!isProjectADherkinProject(project, dir)) return false;

    // whatever directory we are in has some gherkin files
    if (dir.isDirectory()) {
      if (!FileTypeIndex.containsFileOfType(GherkinFileType.INSTANCE, GlobalSearchScopesCore.directoryScope(project, dir, true))) {
        return false;
      }
    }

    if (weAreInsideATestFolder(project, dir)) {
      return setupRunnerParametersForFolder(params, dir);
    }

    return false;
  }

  public boolean weAreInsideATestFolder(@NotNull final Project project, @NotNull  VirtualFile dir) {
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, dir);

    final VirtualFile pubspec = urlResolver.getPubspecYamlFile();
    final VirtualFile rootDir = pubspec == null ? null : pubspec.getParent();
    final VirtualFile testDir = rootDir == null ? null : rootDir.findChild("test");
    final VirtualFile testDriverDir = rootDir == null ? null : rootDir.findChild("test_driver");
    // if both the test and test_driver folders are non existent or not directories, fail
    if ((testDir == null || !testDir.isDirectory()) && (testDriverDir == null || !testDriverDir.isDirectory())) return false;

    if ((testDir != null && VfsUtilCore.isAncestor(testDir, dir, false)) ||
      (testDriverDir != null && VfsUtilCore.isAncestor(testDriverDir, dir, false))) {
      return true;
    }

    return false;
  }

  private boolean setupRunnerParametersForFolder(@NotNull final CucumberDartRunnerParameters params, @NotNull VirtualFile dir) {
    if ("test".equals(dir.getName()) &&
      dir.findChild(PubspecYamlUtil.PUBSPEC_YAML) == null &&
      dir.getParent().findChild(PubspecYamlUtil.PUBSPEC_YAML) != null) {
      dir = dir.getParent(); // anyway test engine looks for tests in 'test' subfolder only
    }

    params.setScope(CucumberDartRunnerParameters.Scope.FOLDER);
    params.setCucumberFilePath(dir.getPath());
    return true;
  }

  private boolean setupRunnerParametersForFileIfApplicable(@NotNull final CucumberDartRunnerParameters params,
                                                                  @NotNull final ConfigurationContext context,
                                                                  @NotNull final Ref<PsiElement> sourceElement) {
    if (context.getPsiLocation() == null) return false;
    if (!(context.getPsiLocation().getContainingFile().getFileType() instanceof GherkinFileType)) {
      return false;
    }

    if (!isProjectADherkinProject(context.getProject(),
      context.getPsiLocation().getContainingFile().getVirtualFile())) return false;


    final PsiFile dartFile = findDherkinRunner(context.getPsiLocation().getContainingFile().getContainingDirectory());

    if (dartFile == null) return false;

    params.setCucumberFilePath(context.getPsiLocation().getContainingFile().getVirtualFile().getPath());
    params.setDartFilePath(dartFile.getVirtualFile().getPath());
    params.setNameFilter(getNameFilter(context));



    final DartClassDefinition clazz = PsiTreeUtil.getChildOfType(dartFile, DartClassDefinition.class);

    return true;
//
//
//    final PsiElement testElement = TestUtil.findTestElement(context.getPsiLocation());
//    if (testElement == null || !setupRunnerParametersForFile(params, testElement)) {
//      return false;
//    }
//
//    sourceElement.set(testElement);
//    return true;
  }

  protected abstract void setScope(CucumberDartRunnerParameters parameters);
  protected abstract String getNameFilter(@NotNull ConfigurationContext context);

  /*
   * walk up the tree trying to find the first dart file we can and use that to run it.
   */
  public static PsiFile findDherkinRunner(PsiDirectory dir) {
    for(PsiFile df : dir.getFiles()) {
      if (df.getFileType() instanceof DartFileType) {
        return df;
      }
    }

    if ("test".equals(dir.getName()) || "test_driver".equals(dir.getName())) {
      return null;
    }

    return findDherkinRunner(dir.getParent());
  }

  public static boolean isProjectADherkinProject(@NotNull final Project project, final VirtualFile file) {
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, file);
    final VirtualFile dherkinTestLib = urlResolver.findFileByDartUrl("package:dherkin2/dherkin.dart");
    if (dherkinTestLib == null) return false;

    final VirtualFile dartTestLib = urlResolver.findFileByDartUrl("package:test/test.dart");
    return (dartTestLib != null);
  }

  public static boolean isFileInTestDirAndTestPackageExists(@NotNull final Project project, @NotNull final VirtualFile file) {
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, file);
    final VirtualFile dartTestLib = urlResolver.findFileByDartUrl("package:test/test.dart");
    if (dartTestLib == null) return false;

    final VirtualFile pubspec = urlResolver.getPubspecYamlFile();
    final VirtualFile rootDir = pubspec == null ? null : pubspec.getParent();
    final VirtualFile testDir = rootDir == null ? null : rootDir.findChild("test");
    return testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, file, true);
  }

  private static boolean setupRunnerParametersForFile(@NotNull final CucumberDartRunnerParameters runnerParams,
                                                      @NotNull final PsiElement psiElement) {
    if (psiElement instanceof DartCallExpression) {
      final String testName = TestUtil.findGroupOrTestName((DartCallExpression)psiElement);
      final List<VirtualFile> virtualFiles = DartResolveUtil.findLibrary(psiElement.getContainingFile());
      if (testName == null || virtualFiles.isEmpty()) {
        return false;
      }

//      runnerParams.setTestName(testName);
//      runnerParams.setScope(DartTestRunnerParameters.Scope.GROUP_OR_TEST_BY_NAME);
      final VirtualFile dartFile = virtualFiles.iterator().next();
      final String dartFilePath = dartFile.getPath();
      runnerParams.setFilePath(dartFilePath);
      return true;
    }
    else {
      final PsiFile psiFile = psiElement.getContainingFile();
      if (psiFile instanceof DartFile) {
        final VirtualFile virtualFile = DartResolveUtil.getRealVirtualFile((DartFile)psiElement);
        if (virtualFile == null || !DartResolveUtil.isLibraryRoot((DartFile)psiElement)) {
          return false;
        }

//        runnerParams.setTestName(DartResolveUtil.getLibraryName((DartFile)psiElement));
//        runnerParams.setScope(DartTestRunnerParameters.Scope.FILE);
        final String dartFilePath = FileUtil.toSystemIndependentName(virtualFile.getPath());
        runnerParams.setFilePath(dartFilePath);
        return true;
      }
    }
    return false;
  }
}
