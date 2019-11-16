package cd.connect.idea.plugins.cucumber.dart.steps.run;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.RunConfigurationProducer;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.Application;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileSystemItem;
import com.intellij.psi.PsiManager;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

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
//      configuration.setGeneratedName();
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

//    if (ensureRunnableDartFile(params, project, virtualDir)) return false;

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

//    if (ensureRunnableDartFile(params, project, sourceFile)) return false;

    params.setCucumberFilePath(sourceFile.getPath());
    params.setNameFilter(getNameFilter(context));

    setScope(params);

    return true;
  }

//  private boolean ensureRunnableDartFile(@NotNull CucumberDartRunnerParameters params, Project project, VirtualFile sourceFile) {
//    // if there is no dart run file or the dart run file is auto-generated, always auto-generate it.
//
//    if (params.getDartFilePath() == null ||
//        (params.getDartFilePath().endsWith(OGURETS_DART_RUNNER) || params.getDartFilePath().endsWith(OGURETS_FLUTTER_TEST_RUNNER))) {
//      PsiFile dartFile = generateRunnableFile(project, sourceFile);
//
//      if (dartFile == null) {
//        return true;
//      }
//
//      params.setDartFilePath(dartFile.getVirtualFile().getPath());
//    }
//
//    return false;
//  }

  protected abstract void setScope(CucumberDartRunnerParameters parameters);
  protected abstract String getNameFilter(@NotNull ConfigurationContext context);

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
    } else if (isFileInTestDirAndTestPackageExists(project, file, "package:flutter_driver/flutter_driver.dart", "test_driver")) {
      params.setFlutterEnabled(true);
      params.setTestType(CucumberDartRunnerParameters.TestType.Integration);
    } else {
      params.setFlutterEnabled(false);
      params.setTestType(CucumberDartRunnerParameters.TestType.Test);
    }

    return true;
  }

  public static boolean isFileInTestDirAndTestPackageExists(
    @NotNull final Project project, @NotNull final VirtualFile file,
        @NotNull final String packageName, @NotNull final String rootDirChild) {
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, file);

    final VirtualFile dartTestLib = urlResolver.findFileByDartUrl(packageName);
    if (dartTestLib == null) return false;

    final VirtualFile pubspec = urlResolver.getPubspecYamlFile();
    final VirtualFile rootDir = pubspec == null ? null : pubspec.getParent();
//    final VirtualFile testDir = rootDir == null ? null : rootDir.findChild("test");
    final VirtualFile testDir = rootDir == null ? null : rootDir.findChild(rootDirChild);
    return testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, file, true);
  }

  static class RunfileConfig {
    List<String> imports = new ArrayList<>();
    List<String> stepClasses = new ArrayList<>();
    List<String> instances = new ArrayList<>();
    String features;

    Project project;
  }

  public static PsiFile generateRunnableFile(@NotNull final Project project, final VirtualFile featureFileOrDir) throws IOException {

    // make sure that all the documents in memory have been saved
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    // now find our pubspec.yaml so we can determine the project directory root
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, featureFileOrDir);

    final VirtualFile pubspec = urlResolver.getPubspecYamlFile();
    final VirtualFile rootDir = pubspec == null ? null : pubspec.getParent();

    // start our config gathering, adding ogurets as the base requirement
    final RunfileConfig config = new RunfileConfig();
//    config.imports.add("import 'package:ogurets/ogurets.dart';");
    config.project = project;
    config.features = featureFileOrDir.getPath().substring(rootDir.getPath().length()+1);

    // we can be a sub-folder in a project and thus this gets complicated, quickly.

    VirtualFile testDir = rootDir == null ? null : rootDir.findChild("test");
    PsiFile runFile = null;
    if (testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, featureFileOrDir, true)) {
      // right, this file is in the $project/test folder, so lets go spelunking down to find all of the stepdefs
      collectStepdefs(config, testDir);
      // now we have to recreate the single ogurets_run.dart file
      PsiDirectory testDirectory = PsiManager.getInstance(project).findDirectory(testDir);
      runFile = createRunFile(testDirectory, OGURETS_DART_RUNNER, config, testDir);
    } else {
      testDir = rootDir == null ? null : rootDir.findChild("test_driver");
      if (testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, featureFileOrDir, true)) {
        // this file is in the test_driver folder, do the same as above but we need to also ensure that FlutterOgurets is included
        collectStepdefs(config, testDir);
        config.stepClasses.add("FlutterHooks"); // to ensure reset occurs
        PsiDirectory testDirectory = PsiManager.getInstance(project).findDirectory(testDir);
        createRunFile(testDirectory, OGURETS_FLUTTER_RUNNER, config, testDir);
        runFile = createRunFile(testDirectory, OGURETS_FLUTTER_TEST_RUNNER, config, testDir);
        return runFile;
      } else if (rootDir != null) { // it isn't in 'test' or 'test_driver', so stick it in the same dir as the feature folder
        VirtualFile featureFolder = featureFileOrDir.getParent().getParent();
        collectStepdefs(config, featureFolder);
        // now we have to recreate the single ogurets_run.dart file
        PsiDirectory testDirectory = PsiManager.getInstance(project).findDirectory(featureFolder);
        runFile = createRunFile(testDirectory, OGURETS_DART_RUNNER, config, featureFolder);

      }
    }

    if (runFile != null) {
      PsiDocumentManager.getInstance(project).commitAllDocuments();
      return runFile;
    }

    return null;
  }

  /**
   * walk down the tree and find all of the files ready.
   * 
   * @param config
   * @param testDir
   */
  public static void collectStepdefs(RunfileConfig config, VirtualFile testDir) {
    int offsetLength = testDir.getPath().length()+1;


    VfsUtilCore.visitChildrenRecursively(testDir, new VirtualFileVisitor<VirtualFile>() {
      @Override
      public boolean visitFile(@NotNull VirtualFile f) {
        if (!f.isDirectory()) {
          if (f.getName().toLowerCase().endsWith(".dart")) {
            PsiFile file = PsiManager.getInstance(config.project).findFile(f);
            // get the non-private classes
            List<DartClassDefinition> classes =
              PsiTreeUtil.findChildrenOfType(file, DartClassDefinition.class).stream().filter(c -> c.getName() != null && !c.getName().startsWith("_")).collect(Collectors.toList());

            if (classes.size() > 0) {
              String importPath = f.getPath().substring(offsetLength);
              // import the file with an alias
              String fileAlias = f.getName().substring(0, f.getName().length() - 5);
              // basePath already has a / at the end
              config.imports.add(String.format("import '%s' as %s;", importPath, fileAlias));

              classes.forEach(c -> config.stepClasses.add(String.format("%s.%s", fileAlias, c.getName())));
            }
          }
        }
        return true;
      }
    });
  }

  private final static String OGURETS_DART_RUNNER = "ogurets_run.dart";
  private final static String OGURETS_FLUTTER_RUNNER = "ogurets_flutter.dart";
  private final static String OGURETS_FLUTTER_TEST_RUNNER = "ogurets_flutter_test.dart";

  public static PsiFile createRunFile(PsiDirectory dir, String template, RunfileConfig config, VirtualFile testDir) throws IOException {
    VirtualFile existingFile = testDir.findChild(template);
    if (existingFile != null && existingFile.exists()) {
      ApplicationManager.getApplication().runWriteAction(() -> {
        try {
          existingFile.delete(template);
        } catch (IOException e) {
        }
      });
    }
    FileTemplateDescriptor fileTemplateDescriptor = new FileTemplateDescriptor(template);
    FileTemplate fileTemplate = FileTemplateManager.getInstance(dir.getProject()).getCodeTemplate(fileTemplateDescriptor.getFileName());

    try {
      Properties properties = new Properties();

      properties.put("IMPORTS", String.join("\n", config.imports));
      properties.put("STEPS", config.stepClasses.stream().map(s -> String.format("  ..step(%s)\n", s)).collect(Collectors.joining()));
      properties.put("FLUTTER_TEST", config.features);

      PsiFile file = FileTemplateUtil.createFromTemplate(fileTemplate, template, properties, dir).getContainingFile();
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile != null) {
        FileEditorManager.getInstance(file.getProject()).openFile(virtualFile, true);
      }

      return file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
