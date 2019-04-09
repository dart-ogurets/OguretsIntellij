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
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import com.jetbrains.lang.dart.util.PubspecYamlUtil;
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
    if (getFileToRun(context) == null) {
      return false;
    }

    final boolean ok;

    final PsiElement location = context.getPsiLocation();
    if (location instanceof PsiDirectory) {
      ok = setupRunnerParametersForFolderIfApplicable(configuration.getProject(), configuration.getRunnerParameters(),
        ((PsiDirectory)location));
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
                                                                    @NotNull final PsiDirectory dir) {

    VirtualFile virtualDir = dir.getVirtualFile();
    if (!isTestableProject(params, project, virtualDir)) return false;

    // whatever directory we are in has some gherkin files
    if (dir.isDirectory()) {
      if (!FileTypeIndex.containsFileOfType(GherkinFileType.INSTANCE, GlobalSearchScopesCore.directoryScope(project, virtualDir, true))) {
        return false;
      }
    }

    final PsiFile dartFile = findDherkinRunner(dir);

    if (dartFile == null) return false;

    params.setDartFilePath(dartFile.getVirtualFile().getPath());
    setScope(params);
    params.setCucumberFilePath(virtualDir.getPath());

    return true;
  }

  private boolean setupRunnerParametersForFileIfApplicable(@NotNull final CucumberDartRunnerParameters params,
                                                                  @NotNull final ConfigurationContext context,
                                                                  @NotNull final Ref<PsiElement> sourceElement) {
    if (context.getPsiLocation() == null) return false;
    if (!(context.getPsiLocation().getContainingFile().getFileType() instanceof GherkinFileType)) {
      return false;
    }

    Project project = context.getProject();
    VirtualFile sourceFile = context.getPsiLocation().getContainingFile().getVirtualFile();
    
    if (!isTestableProject(params, project, sourceFile)) return false;


    final PsiFile dartFile = findDherkinRunner(context.getPsiLocation().getContainingFile().getContainingDirectory());

    if (dartFile == null) return false;

    params.setCucumberFilePath(sourceFile.getPath());
    params.setDartFilePath(dartFile.getVirtualFile().getPath());
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
  public static PsiFile findDherkinRunner(@Nullable PsiDirectory dir) {
    if (dir == null) {
      return null;
    }

    for(PsiFile df : dir.getFiles()) {
      if (df.getFileType() instanceof DartFileType && df.getName().endsWith(".config.dart")) {
        return df;
      }
    }

    if ("test".equals(dir.getName()) || "test_driver".equals(dir.getName())) {
      return null;
    }

    return findDherkinRunner(dir.getParent());
  }


  private boolean isTestableProject(@NotNull final CucumberDartRunnerParameters params,
                                    @NotNull Project project,
                                    @NotNull final VirtualFile file) {
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, file);

    final VirtualFile dherkinTestLib = urlResolver.findFileByDartUrl("package:dherkin2/dherkin.dart");

    if (dherkinTestLib == null) {
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
