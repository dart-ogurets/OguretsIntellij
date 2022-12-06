package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.Location;
import com.intellij.execution.actions.ConfigurationContext;
import com.intellij.execution.actions.LazyRunConfigurationProducer;
import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeUtil;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Comparing;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileVisitor;
import com.intellij.psi.*;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScopesCore;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import dev.bluebiscuitdesign.cucumber.dart.CucumberDartUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.psi.GherkinFileType;
import org.jetbrains.plugins.cucumber.psi.impl.GherkinFileImpl;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

abstract public class CucumberDartRunConfigurationProducer extends LazyRunConfigurationProducer<CucumberDartRunConfiguration> {

    @NotNull
    @Override
    public ConfigurationFactory getConfigurationFactory() {
        return ConfigurationTypeUtil.findConfigurationType(CucumberDartRunConfigurationType.class).getConfigurationFactories()[0];
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
            ok = setupRunnerParametersForFolderIfApplicable(configuration.getProject(), configuration.getRunnerParameters(), ((PsiDirectory) file));
        } else {
            ok = setupRunnerParametersForFileIfApplicable(configuration.getRunnerParameters(), context, (PsiFile) file);
        }
        if (ok) {
            configuration.setName(getConfigurationName(context));
            // configuration.setGeneratedName();
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
        if (file == null) {
            return false;
        }
        boolean ok;
        CucumberDartRunnerParameters paramsForContext = new CucumberDartRunnerParameters();
        if (file instanceof PsiDirectory) {
            ok = setupRunnerParametersForFolderIfApplicable(context.getProject(), paramsForContext, ((PsiDirectory) file));
        } else {
            ok = setupRunnerParametersForFileIfApplicable(paramsForContext, context, (PsiFile) file);
        }
        if (!ok) {
            return false;
        }
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
        if (!isTestableProject(params, project, virtualDir)) {
            return false;
        }
        // whatever directory we are in has some gherkin files
        if (dir.isDirectory()) {
            if (!FileTypeIndex.containsFileOfType(GherkinFileType.INSTANCE, GlobalSearchScopesCore.directoryScope(project, virtualDir, true))) {
                return false;
            }
        }
        setScope(params);
        params.setCucumberFilePath(virtualDir.getPath());
        return true;
    }

    private boolean setupRunnerParametersForFileIfApplicable(@NotNull final CucumberDartRunnerParameters params, @NotNull final ConfigurationContext context, @NotNull PsiFile file) {
        if (file.getContainingFile() == null || !(file.getContainingFile().getFileType() instanceof GherkinFileType)) {
            return false;
        }
        Project project = context.getProject();
        VirtualFile sourceFile = file.getVirtualFile();
        if (!isTestableProject(params, project, sourceFile)) {
            return false;
        }
        params.setCucumberFilePath(sourceFile.getPath());
        params.setNameFilter(getNameFilter(context));
        setScope(params);
        return true;
    }

    protected abstract void setScope(CucumberDartRunnerParameters parameters);

    protected abstract String getNameFilter(@NotNull ConfigurationContext context);

    private boolean isTestableProject(@NotNull final CucumberDartRunnerParameters params, @NotNull Project project, @NotNull final VirtualFile file) {
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

    public static boolean isFileInTestDirAndTestPackageExists(@NotNull final Project project, @NotNull final VirtualFile file, @NotNull final String packageName, @NotNull final String rootDirChild) {
        final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, file);
        final VirtualFile dartTestLib = urlResolver.findFileByDartUrl(packageName);
        if (dartTestLib == null) {
            return false;
        }
        final VirtualFile pubspec = urlResolver.getPubspecYamlFile();
        final VirtualFile rootDir = pubspec == null ? null : pubspec.getParent();
        final VirtualFile testDir = rootDir == null ? null : rootDir.findChild(rootDirChild);
        return testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, file, true);
    }

    static class RunfileConfig {
        Set<String> imports = new HashSet<>();
        Set<String> stepClasses = new HashSet<>();
        Set<String> instances = new HashSet<>();
        String features;
        Project project;
    }

    public static PsiFile generateRunnableFile(@NotNull final Project project, final VirtualFile featureFileOrDir) throws IOException {
        // make sure that all the documents in memory have been saved
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        // now find our pubspec.yaml, so we can determine the project directory root
        final DartUrlResolver urlResolver = DartUrlResolver.getInstance(project, featureFileOrDir);
        final VirtualFile pubspec = urlResolver.getPubspecYamlFile();
        final VirtualFile rootDir = pubspec == null ? null : pubspec.getParent();
        // start our config gathering, adding ogurets as the base requirement
        final RunfileConfig config = new RunfileConfig();
        config.project = project;
        config.features = featureFileOrDir.getPath().substring(rootDir.getPath().length() + 1);

        // collect the imported packages that are known to contain step definitions used by the feature file(s)
        PsiManager manager = PsiManager.getInstance(project);
        Set<PsiFile> stepDefFiles = getStepDefFiles(featureFileOrDir, manager);
        Set<VirtualFile> knownPackages = new HashSet<>();
        urlResolver.processLivePackages((packageName, virtualFile) -> knownPackages.add(virtualFile));

        // we can be in a sub-folder in a project and thus this gets complicated, quickly.
        VirtualFile testDir = rootDir == null ? null : rootDir.findChild("test");
        PsiFile runFile = null;
        if (testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, featureFileOrDir, true)) {
            // right, this file is in the $project/test folder, so lets go spelunking down to find all of the stepdefs
            collectStepdefs(config, testDir, stepDefFiles, knownPackages);
            // now we have to recreate the single ogurets_run.dart file
            PsiDirectory testDirectory = PsiManager.getInstance(project).findDirectory(testDir);
            runFile = createRunFile(testDirectory, OGURETS_DART_RUNNER, config, testDir);
        } else {
            testDir = rootDir == null ? null : rootDir.findChild("test_driver");
            if (testDir != null && testDir.isDirectory() && VfsUtilCore.isAncestor(testDir, featureFileOrDir, true)) {
                // this file is in the test_driver folder, do the same as above but we need to also ensure that FlutterOgurets is included
                collectStepdefs(config, testDir, stepDefFiles, knownPackages);
                config.stepClasses.add("FlutterHooks"); // to ensure reset occurs
                PsiDirectory testDirectory = PsiManager.getInstance(project).findDirectory(testDir);
                createRunFile(testDirectory, OGURETS_FLUTTER_RUNNER, config, testDir);
                runFile = createRunFile(testDirectory, OGURETS_FLUTTER_TEST_RUNNER, config, testDir);
                return runFile;
            } else if (rootDir != null) {
                // it isn't in 'test' or 'test_driver', so stick it in the same dir as the feature folder
                VirtualFile featureFolder = featureFileOrDir.getParent().getParent();
                collectStepdefs(config, featureFolder, stepDefFiles, knownPackages);
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

    @NotNull
    private static Set<PsiFile> getStepDefFiles(VirtualFile featureFileOrDir, PsiManager manager) {
        Set<PsiFile> stepDefFiles = new HashSet<>();
        if (featureFileOrDir.isDirectory()) {
            for (VirtualFile file : featureFileOrDir.getChildren()) {
                if (file.isDirectory()) {
                    stepDefFiles.addAll(getStepDefFiles(file, manager));
                } else {
                    if (file.getName().endsWith(".feature")) {
                        addStepDefFiles(manager, stepDefFiles, file);
                    }
                }
            }
        } else {
            addStepDefFiles(manager, stepDefFiles, featureFileOrDir);
        }
        return stepDefFiles;
    }

    private static void addStepDefFiles(PsiManager manager, Set<PsiFile> stepDefFiles, VirtualFile file) {
        FileViewProvider viewProvider = new SingleRootFileViewProvider(manager, file);
        GherkinFile featureFile = new GherkinFileImpl(viewProvider);
        stepDefFiles.addAll(CucumberDartUtil.getStepDefinitionContainers(featureFile));
    }

    /**
     * Get the import package String for the given file.
     * This means we'll have to look for the dart file that exports this one.
     *
     * @param file
     * @return
     */
    private static String getImportPackage(PsiFile file, Set<VirtualFile> knownPackages) {
        // Iteratively look for files in containing directory that might export this file
        PsiDirectory containingDir = file.getContainingDirectory();
        String exportFileMatch = file.getVirtualFile().getPath().substring(containingDir.getVirtualFile().getPath().length() + 1);
        return getImportPackage(file.getContainingDirectory(), exportFileMatch, knownPackages);
    }

    /**
     * Get the import package for a matching export filename within the given containing directory.
     *
     * @param containingDir
     * @param exportFileMatch
     * @param knownPackages
     * @return
     */
    private static String getImportPackage(PsiDirectory containingDir, String exportFileMatch, Set<VirtualFile> knownPackages) {
        for (PsiFile dirFile : containingDir.getFiles()) {
            System.out.println(knownPackages);
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(dirFile.getVirtualFile().getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.startsWith("export") && line.contains(exportFileMatch)) {
                        // Get the package that contains this file...
                        Optional<VirtualFile> containingPackage = knownPackages.stream().filter((pkg) -> dirFile.getVirtualFile().getPath().startsWith(pkg.getPath())).findFirst();
                        if (containingPackage.isPresent()) {
                            // Get package name from pubspec.yaml
                            VirtualFile pkgYaml = containingPackage.get().getParent().findChild("pubspec.yaml");
                            String packageName = getPackageName(pkgYaml);
                            return "package:" + packageName + "/" + dirFile.getVirtualFile().getName();
                        }
                    }
                }
            } catch (IOException e) {
                // Ignore ...
            }
        }
        // Not yet found? Look in parent directory...
        if (containingDir.getParent() != null) {
            return getImportPackage(containingDir.getParent(), exportFileMatch, knownPackages);
        }
        return null;
    }

    private static String getPackageName(VirtualFile pkgYaml) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(pkgYaml.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("name:")) {
                    return line.substring(line.indexOf(":") + 1).trim();
                }
            }
        } catch (IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Find all files containing step definitions being used by the feature.
     * Also look for step definitions in imported packages.
     *
     * @param config
     * @param testDir
     */
    public static void collectStepdefs(RunfileConfig config,
                                       VirtualFile testDir,
                                       Collection<? extends PsiFile> stepDefFiles,
                                       Set<VirtualFile> knownPackages) {
        int offsetLength = testDir.getPath().length() + 1;
        Set<String> rawImportPaths = new HashSet<>();
        // First look into all files that are actually used by the step definitions in the given feature.
        stepDefFiles.forEach(file -> {
            VirtualFile f = file.getVirtualFile();
            // get the non-private classes
            List<DartClassDefinition> classes =
                    PsiTreeUtil.findChildrenOfType(file, DartClassDefinition.class).stream()
                            .filter(c -> c.getName() != null && !c.getName().startsWith("_"))
                            .toList();
            if (classes.size() > 0) {
                String importPath = f.getPath().startsWith(testDir.getPath()) ?
                        f.getPath().substring(offsetLength) :
                        getImportPackage(file, knownPackages);
                // import the file with an alias
                if (null == importPath) {
                    return;
                }
                rawImportPaths.add(f.getPath());
                addImportAndClasses(config, f, classes, importPath);
            }
        });

        // Still, we also descend into the package's local tree structure,
        // just to make sure we don't miss any "non-step" annotated stuff from within the package...
        VfsUtilCore.visitChildrenRecursively(testDir, new VirtualFileVisitor<VirtualFile>() {
            @Override
            public boolean visitFile(@NotNull VirtualFile f) {
                if (!f.isDirectory()) {
                    if (f.getName().toLowerCase().endsWith(".dart")) {
                        PsiFile file = PsiManager.getInstance(config.project).findFile(f);
                        // get the non-private classes
                        List<DartClassDefinition> classes =
                                PsiTreeUtil.findChildrenOfType(file, DartClassDefinition.class).stream()
                                        .filter(c -> c.getName() != null && !c.getName().startsWith("_"))
                                        .toList();

                        if (classes.size() > 0 && !rawImportPaths.contains(f.getPath())) {
                            String importPath = f.getPath().substring(offsetLength);
                            // import the file with an alias
                            addImportAndClasses(config, f, classes, importPath);
                        }
                    }
                }
                return true;
            }
        });
    }

    private static void addImportAndClasses(RunfileConfig config, VirtualFile f, List<DartClassDefinition> classes, String importPath) {
        String fileAlias = toDartFileAlias(f.getName().substring(0, f.getName().length() - 5)).replaceAll("\\.", "_");
        // basePath already has a / at the end
        config.imports.add(String.format("import '%s' as %s;", importPath, fileAlias));

        classes.forEach(c -> config.stepClasses.add(String.format("%s.%s", fileAlias, c.getName())));
    }

    // dart wants it file aliases as underscores, ref: Effective Dart
    private static String toDartFileAlias(String fileAlias) {
        StringBuilder dartFileAlias = new StringBuilder();
        for (char c : fileAlias.toCharArray()) {
            if (Character.isUpperCase(c)) {
                dartFileAlias.append("_");
            }
            dartFileAlias.append(Character.toLowerCase(c));
        }
        String alias = dartFileAlias.toString();
        return alias.startsWith("_") ? alias.substring(1) : alias;
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
            properties.put("STEPS", config.stepClasses.stream().map(s -> String.format("\n\t..step(%s)", s)).collect(Collectors.joining()));
            properties.put("FLUTTER_TEST", config.features);
            PsiFile file = FileTemplateUtil.createFromTemplate(fileTemplate, template, properties, dir).getContainingFile();
            return file;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
