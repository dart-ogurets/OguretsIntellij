package cd.connect.idea.plugins.cucumber.dart.steps.run;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;

abstract public class CucumberDartRunConfigurationProducer extends RunConfigurationProducer<CucumberDartRunConfiguration> {
  public CucumberDartRunConfigurationProducer() {
    super(CucumberDartRunConfigurationType.class);
  }

  @Override
  protected boolean setupConfigurationFromContext(final @NotNull CucumberDartRunConfiguration configuration,
                                                  final @NotNull ConfigurationContext context,
                                                  final @NotNull Ref<PsiElement> sourceElement) {
    PsiFileSystemItem file = getPsiFileToRun(context);

    if (file == null) {
      return false;
    }

    final boolean ok;

    if (file instanceof PsiDirectory) {
      ok = setupRunnerParametersForFolderIfApplicable(configuration.getProject(), configuration.getRunnerParameters(),
        ((PsiDirectory)file));
    }
    else {
      ok = setupRunnerParametersForFileIfApplicable(configuration.getRunnerParameters(), context, (PsiFile)file);
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

    final PsiFileSystemItem file = getPsiFileToRun(context);

    if (file == null) return false;

    boolean ok = false;
    CucumberDartRunnerParameters paramsForContext = new CucumberDartRunnerParameters();

    if (file instanceof PsiDirectory) {
      ok = setupRunnerParametersForFolderIfApplicable(context.getProject(), paramsForContext,
        ((PsiDirectory)file));
    }
    else {
      ok = setupRunnerParametersForFileIfApplicable(paramsForContext, context, (PsiFile)file);
    }

    if (!ok) return false;

    if (!Comparing.strEqual(getConfigurationName(context), runConfiguration.getName())) {
      return false;
    }

    if (!Comparing.strEqual(paramsForContext.getNameFilter(), runConfiguration.getRunnerParameters().getNameFilter())) {
      return false;
    }

    if (!Comparing.strEqual(paramsForContext.getCucumberFilePath(), runConfiguration.getRunnerParameters().getCucumberFilePath())) {
      return false;
    }

    if (runConfiguration.getRunnerParameters().getCucumberScope() != paramsForContext.getCucumberScope()) {
      return false;
    }

    if (!Comparing.strEqual(runConfiguration.getRunnerParameters().getDartFilePath(), paramsForContext.getDartFilePath())) {
      return false;
    }

    return true;
  }

  @Nullable
  protected abstract PsiFileSystemItem getPsiFileToRun(ConfigurationContext context);

  @Nullable
  protected VirtualFile getFileToRun(ConfigurationContext context) {
    final PsiFileSystemItem psiFile = getPsiFileToRun(context);
    return psiFile != null ? psiFile.getVirtualFile() : null;
  }

  // ensure the dart test package + ogurets is available to us
  private boolean setupRunnerParametersForFolderIfApplicable(@NotNull final Project project,
                                                                    @NotNull final CucumberDartRunnerParameters params,
                                                                    @NotNull final PsiDirectory dir) {

    VirtualFile virtualDir = dir.getVirtualFile();
    if (!isTestableProject(params, project, virtualDir)) return false;

    // whatever directory we are in has some gherkin files
    if (dir.isDirectory()) {
      if (!FileTypeIndex.containsFileOfType(GherkinFileType.INSTANCE, GlobalSearchScopesCore.directoryScope(project, virtualDir, true))) {
        return false;
      }
    }

    final PsiFile dartFile = findOguretsRunner(dir);

    if (dartFile == null) return false;

    params.setDartFilePath(dartFile.getVirtualFile().getPath());
    setScope(params);
    params.setCucumberFilePath(virtualDir.getPath());

    return true;
  }

  private boolean setupRunnerParametersForFileIfApplicable(@NotNull final CucumberDartRunnerParameters params,
                                                           @NotNull final ConfigurationContext context,
                                                           @NotNull PsiFile file) {
    if (file.getContainingFile() == null || !(file.getContainingFile().getFileType() instanceof GherkinFileType)) {
      return false;
    }

    Project project = context.getProject();
    VirtualFile sourceFile = file.getVirtualFile();
    
    if (!isTestableProject(params, project, sourceFile)) return false;

    if (params.getDartFilePath() == null) {
      final PsiFile dartFile = findOguretsRunner(file.getContainingFile().getContainingDirectory());

      if (dartFile == null) return false;
      params.setDartFilePath(dartFile.getVirtualFile().getPath());
    }

    params.setCucumberFilePath(sourceFile.getPath());
    params.setNameFilter(getNameFilter(context));

    setScope(params);

//    final DartClassDefinition clazz = PsiTreeUtil.getChildOfType(dartFile, DartClassDefinition.class);

    return true;
  }

  protected abstract void setScope(CucumberDartRunnerParameters parameters);
  protected abstract String getNameFilter(@NotNull ConfigurationContext context);

  /*
   * walk up the tree trying to find the first dart file we can and use that to run it.
   */
  @Nullable
  public static PsiFile findOguretsRunner(@Nullable PsiDirectory dir) {
    if (dir == null) {
      return null;
    }

    for(PsiFile df : dir.getFiles()) {
      if (df.getFileType() instanceof DartFileType && df.getName().endsWith("_test.dart")) {
        return df;
      }
    }

    if ("test".equals(dir.getName()) || "test_driver".equals(dir.getName())) {
      return null;
    }

    return findOguretsRunner(dir.getParent());
  }


  private boolean isTestableProject(@NotNull final CucumberDartRunnerParameters params,
                                    @NotNull Project project,
                                    @NotNull final VirtualFile file) {
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, file);

    final VirtualFile oguretsTestLib = urlResolver.findFileByDartUrl("package:ogurets/ogurets.dart");

    if (oguretsTestLib == null) {
      return false;
    }

    if (isFileInTestDirAndTestPackageExists(project, file, "package:flutter_test/flutter_test.dart", "test")) {
      params.setFlutterEnabled(true);
      params.setTestType(CucumberDartRunnerParameters.TestType.Test);
    } else if (isFileInTestDirAndTestPackageExists(project, file, "package:test/test.dart", "test")) {
      params.setFlutterEnabled(false);
      params.setTestType(CucumberDartRunnerParameters.TestType.Test);
    } else if (isFileInTestDirAndTestPackageExists(project, file, "package:flutter_driver/flutter_driver.dart", "test_driver")) {
      params.setFlutterEnabled(true);
      params.setTestType(CucumberDartRunnerParameters.TestType.Integration);
    } else {
      return false;
    }

    return true;
  }

  public static boolean isFileInTestDirAndTestPackageExists(
    @NotNull final Project project, @NotNull final VirtualFile file,
        @NotNull final String packageName, @NotNull final String rootDirChild) {
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, file);
//    final VirtualFile dartTestLib = urlResolver.findFileByDartUrl("package:test/test.dart");
    final VirtualFile dartTestLib = urlResolver.findFileByDartUrl(packageName);
    if (dartTestLib == null) return false;

    final VirtualFile pubspec = urlResolver.getPubspecYamlFile();
    final VirtualFile rootDir = pubspec == null ? null : pubspec.getParent();
//    final VirtualFile testDir = rootDir == null ? null : rootDir.findChild("test");
    final VirtualFile testDir = rootDir == null ? null : rootDir.findChild(rootDirChild);
    return testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, file, true);
  }
}
