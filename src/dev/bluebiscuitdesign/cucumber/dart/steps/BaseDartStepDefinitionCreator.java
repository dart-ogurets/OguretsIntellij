package dev.bluebiscuitdesign.cucumber.dart.steps;

import dev.bluebiscuitdesign.cucumber.dart.CucumberDartUtil;
import com.intellij.ide.actions.CreateFileAction;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.ide.fileTemplates.FileTemplateUtil;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Ref;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.util.ObjectUtils;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.AbstractStepDefinitionCreator;
import org.jetbrains.plugins.cucumber.CucumberBundle;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;

import java.util.Arrays;
import java.util.Properties;

import static dev.bluebiscuitdesign.cucumber.dart.steps.run.CucumberDartRunConfigurationProducer.isFileInTestDirAndTestPackageExists;

abstract public class BaseDartStepDefinitionCreator extends AbstractStepDefinitionCreator {
  private static final String STEP_DEFINITION_SUFFIX = "MyStepdefs";
  private static final String DART_TEMPLATE = "Dart File.dart";

  private final static Logger LOG = Logger.getInstance("#" + BaseDartStepDefinitionCreator.class.getName());
  private static final String CONSTRUCTOR = "CONSTRUCTOR";
  private static final String IMPORTS = "IMPORTS";

  @NotNull
  @Override
  public PsiFile createStepDefinitionContainer(@NotNull PsiDirectory dir, @NotNull String name) {
    FileTemplateDescriptor fileTemplateDescriptor = new FileTemplateDescriptor(DART_TEMPLATE);
    FileTemplate fileTemplate = FileTemplateManager.getInstance(dir.getProject()).getCodeTemplate(fileTemplateDescriptor.getFileName());

    VirtualFile destDir = dir.getVirtualFile();
    final DartUrlResolver urlResolver = DartUrlResolver.getInstance(dir.getProject(), destDir);

    final VirtualFile oguretsTestLib = urlResolver.findFileByDartUrl("package:ogurets/ogurets.dart");

    if (oguretsTestLib == null) {
      throw new RuntimeException("Ogurets missing!");
    }

    try {
      CreateFileAction.MkDirs mkdirs = new CreateFileAction.MkDirs(name, dir);
      name = mkdirs.newName;
      dir = mkdirs.directory;

      Properties properties = new Properties();
      properties.setProperty("CLASS_NAME", name);
      if (isFileInTestDirAndTestPackageExists(dir.getProject(), destDir,
        "package:ogurets_flutter/ogurets_flutter.dart", "test_driver")) {
        properties.put(IMPORTS, "import 'package:ogurets_flutter/ogurets_flutter.dart';");
        properties.put(CONSTRUCTOR, "  FlutterOgurets _world;\n\n  " + name + "(this._world);\n");
      } else {
        properties.put(IMPORTS, "");
        properties.put(CONSTRUCTOR, "");
      }

      PsiFile file = FileTemplateUtil.createFromTemplate(fileTemplate, name, properties, dir).getContainingFile();

      // stop focusing on this file, it drives me crazy!
//      VirtualFile virtualFile = file.getVirtualFile();
//      if (virtualFile != null) {
//        FileEditorManager.getInstance(file.getProject()).openFile(virtualFile, true);
//      }

      return file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public boolean validateNewStepDefinitionFileName(@NotNull final Project project, @NotNull final String name) {
    if (name.length() == 0) return false;
    if (!Character.isJavaIdentifierStart(name.charAt(0))) return false;
    for (int i = 1; i < name.length(); i++) {
      if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
    }
    return true;
  }

  @NotNull
  @Override
  public String getDefaultStepDefinitionFolderPath(@NotNull final GherkinStep step) {
    @NotNull PsiFile featureFile = step.getContainingFile();

    if (featureFile != null) {
      PsiDirectory psiDirectory = featureFile.getContainingDirectory();

      while (psiDirectory != null && !"features".equals(psiDirectory.getName())) {
        psiDirectory = psiDirectory.getParent();
      }

      if (psiDirectory == null) {
        psiDirectory = featureFile.getContainingDirectory().getParent();
      } else {
        psiDirectory = psiDirectory.getParent(); // found the features directory
      }

      if (psiDirectory == null) {
        psiDirectory = featureFile.getContainingDirectory();
      }

      PsiDirectory stepDefs = Arrays.stream(psiDirectory.getSubdirectories())
        .filter(sd -> sd.getName().equals("steps"))
        .findFirst()
        .orElse(null);

      if (stepDefs == null) {
        final PsiDirectory featureParentDir = psiDirectory;
        final Ref<PsiDirectory> dirRef = new Ref<>();
        WriteCommandAction.writeCommandAction(step.getProject())
          .withName(CucumberBundle.message("cucumber.quick.fix.create.step.command.name.add")).run(() -> {
          // create steps_definitions directory
          dirRef.set(featureParentDir.createSubdirectory("steps"));
        });
      }

      return stepDefs.getName();
    }

    return featureFile.getParent().getName();
  }

  public static String processGeneratedStepDefinition(@NotNull String stepDefinition, @NotNull PsiElement context) {
    return stepDefinition
          .replace("Integer ", "int ")
          .replace("PendingException", CucumberDartUtil.getCucumberPendingExceptionFqn(context))
          .replace('"', '\'');
  }

  @NotNull
  @Override
  public String getDefaultStepFileName(@NotNull final GherkinStep step) {
    return STEP_DEFINITION_SUFFIX;
  }

}

