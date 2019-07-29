package cd.connect.idea.plugins.cucumber.dart.steps;

import cd.connect.idea.plugins.cucumber.dart.CucumberDartUtil;
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

import static cd.connect.idea.plugins.cucumber.dart.steps.run.CucumberDartRunConfigurationProducer.isFileInTestDirAndTestPackageExists;

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
      VirtualFile virtualFile = file.getVirtualFile();
      if (virtualFile != null) {
        FileEditorManager.getInstance(file.getProject()).openFile(virtualFile, true);
      }

      return file;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
//
//    PsiClass newClass = CreateClassUtil.createClassNamed(name, DART_TEMPLATE, dir);
////      CreateClassUtil.createClassNamed(name, CreateClassUtil.DEFAULT_CLASS_TEMPLATE, dir);
//    assert newClass != null;
//    return newClass.getContainingFile();
  }

//  @Override
//  public boolean createStepDefinition(@NotNull GherkinStep step, @NotNull PsiFile file) {
//    if (!(file instanceof PsiClassOwner)) return false;
//
//    final Project project = file.getProject();
//    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
//    assert editor != null;
//
//    closeActiveTemplateBuilders(file);
//
//    final PsiClass clazz = PsiTreeUtil.getChildOfType(file, PsiClass.class);
//    if (clazz != null) {
//      PsiDocumentManager.getInstance(project).commitAllDocuments();
//
//      // snippet text
//      final PsiMethod element = buildStepDefinitionByStep(step, file.getLanguage());
//      PsiMethod addedElement = (PsiMethod)clazz.add(element);
//      addedElement = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(addedElement);
//
//      // TODO:  should replace with Dart, need to find
//      JavaCodeStyleManager.getInstance(project).shortenClassReferences(addedElement);
//      editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
//      assert editor != null;
//
//      final PsiParameterList blockVars = addedElement.getParameterList();
//      final PsiCodeBlock body = addedElement.getBody();
//      final PsiAnnotation annotation = addedElement.getModifierList().getAnnotations()[0];
//      final PsiElement regexpElement = annotation.getParameterList().getAttributes()[0];
//
//      runTemplateBuilderOnAddedStep(editor, addedElement, regexpElement, blockVars, body);
//    }
//
//    return true;
//  }

//  void runTemplateBuilderOnAddedStep(@NotNull Editor editor,
//                                     @NotNull PsiElement addedElement,
//                                     PsiElement regexpElement,
//                                     PsiParameterList blockVars,
//                                     PsiCodeBlock body) {
//    Project project = regexpElement.getProject();
//    final TemplateBuilderImpl builder = (TemplateBuilderImpl)TemplateBuilderFactory.getInstance().createTemplateBuilder(addedElement);
//
//    final TextRange range = new TextRange(1, regexpElement.getTextLength() - 1);
//    builder.replaceElement(regexpElement, range, regexpElement.getText().substring(range.getStartOffset(), range.getEndOffset()));
//
//    for (PsiParameter var : blockVars.getParameters()) {
//      final PsiElement nameIdentifier = var.getNameIdentifier();
//      if (nameIdentifier != null) {
//        builder.replaceElement(nameIdentifier, nameIdentifier.getText());
//      }
//    }
//
//    if (body.getStatements().length > 0) {
//      final PsiElement firstStatement = body.getStatements()[0];
//      final TextRange pendingRange = new TextRange(0, firstStatement.getTextLength() - 1);
//      builder.replaceElement(firstStatement, pendingRange,
//                             firstStatement.getText().substring(pendingRange.getStartOffset(), pendingRange.getEndOffset()));
//    }
//
//    Template template = builder.buildInlineTemplate();
//
//    final PsiDocumentManager documentManager = PsiDocumentManager.getInstance(project);
//    documentManager.doPostponedOperationsAndUnblockDocument(editor.getDocument());
//
//    editor.getCaretModel().moveToOffset(addedElement.getTextRange().getStartOffset());
//    TemplateEditingAdapter adapter = new TemplateEditingAdapter() {
//        @Override
//        public void templateFinished(@NotNull Template template, boolean brokenOff) {
//          ApplicationManager.getApplication().runWriteAction(() -> {
//            PsiDocumentManager.getInstance(project).commitDocument(editor.getDocument());
//            PsiFile psiFile = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
//            if (psiFile == null) {
//              return;
//            }
//            int offset = editor.getCaretModel().getOffset() - 1;
//            PsiCodeBlock codeBlock = null;
//            PsiLambdaExpression lambda = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, PsiLambdaExpression.class, false);
//            if (lambda != null) {
//              PsiElement body = lambda.getBody();
//              codeBlock = body instanceof PsiCodeBlock ? (PsiCodeBlock)body : null;
//            }
//            if (codeBlock == null) {
//              PsiMethod method = PsiTreeUtil.findElementOfClassAtOffset(psiFile, offset, PsiMethod.class, false);
//              if (method != null) {
//                codeBlock = method.getBody();
//              }
//            }
//
//            if (codeBlock != null) {
//              CreateFromUsageUtils.setupEditor(codeBlock, editor);
//            }
//          });
//        }
//      };
//
//    TemplateManager.getInstance(project).startTemplate(editor, template, adapter);
//  }

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
  public PsiDirectory getDefaultStepDefinitionFolder(@NotNull final GherkinStep step) {
    PsiFile featureFile = step.getContainingFile();
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

      return ObjectUtils.assertNotNull(stepDefs);
    }

    assert featureFile != null;
    return ObjectUtils.assertNotNull(featureFile.getParent());
  }

//  @NotNull
//  @Override
//  public String getStepDefinitionFilePath(@NotNull final PsiFile file) {
//    return file.getName();
//  }

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

//  private static PsiMethod buildStepDefinitionByStep(@NotNull final GherkinStep step, Language language) {
//    String annotationPackage = new AnnotationPackageProvider().getAnnotationPackageFor(step);
//    String methodAnnotation = String.format("@%s.", annotationPackage);
//
//    final PickleStep cucumberStep = new PickleStep(step.getStepName(), new ArrayList<>(), new ArrayList<>());
//    final SnippetGenerator generator = new SnippetGenerator(new DartSnippet());
//
//    String snippet = generator.getSnippet(cucumberStep, step.getKeyword().getText(), new FunctionNameGenerator(new CamelCaseConcatenator()));
//
//    if (CucumberDartUtil.isCucumberExpressionsAvailable(step)) {
//      snippet = replaceRegexpWithCucumberExpression(snippet, step.getStepName());
//    }
//
//    snippet = snippet.replaceFirst("@", methodAnnotation);
//    snippet = processGeneratedStepDefinition(snippet, step);
//
//    JVMElementFactory factory = JVMElementFactories.requireFactory(language, step.getProject());
//    PsiMethod methodFromCucumberLibraryTemplate = factory.createMethodFromText(snippet, step);
//
//    try {
//      return createStepDefinitionFromSnippet(methodFromCucumberLibraryTemplate, step, factory);
//    } catch (Exception e) {
//      return methodFromCucumberLibraryTemplate;
//    }
//  }

//  private static String replaceRegexpWithCucumberExpression(@NotNull String snippet, @NotNull String step) {
//    try {
//      ParameterTypeRegistry registry = new ParameterTypeRegistry(Locale.getDefault());
//      CucumberExpressionGenerator generator = new CucumberExpressionGenerator(registry);
//      GeneratedExpression result = generator.generateExpressions(step).get(0);
//      if (result != null) {
//        String cucumberExpression = new DartSnippet().escapePattern(result.getSource());
//        String[] lines = snippet.split("\n");
//
//        int start = lines[0].indexOf('(') + 1;
//        lines[0] = lines[0].substring(0, start + 1) + cucumberExpression + "\")";
//        return StringUtil.join(lines, "");
//      }
//    }
//    catch (Exception ignored) {
//      LOG.warn("Failed to replace regex with Cucumber Expression for step: " + step);
//    }
//    return snippet;
//  }
//
//  private static PsiMethod createStepDefinitionFromSnippet(@NotNull PsiMethod methodFromSnippet, @NotNull GherkinStep step,
//                                                           @NotNull JVMElementFactory factory) {
//    PsiAnnotation cucumberStepAnnotation = getCucumberStepAnnotation(methodFromSnippet);
//    String regexp = CucumberDartUtil.getPatternFromStepDefinition(cucumberStepAnnotation);
//    String stepAnnotationName = cucumberStepAnnotation.getQualifiedName();
//    if (stepAnnotationName == null) {
//      stepAnnotationName = DEFAULT_STEP_KEYWORD;
//    }
//
//    FileTemplateDescriptor fileTemplateDescriptor = new FileTemplateDescriptor(FILE_TEMPLATE_CUCUMBER_DART_STEP_DEFINITION_JAVA);
//    FileTemplate fileTemplate = FileTemplateManager.getInstance(step.getProject()).getCodeTemplate(fileTemplateDescriptor.getFileName());
//    String text = fileTemplate.getText();
//    text = text.replace("${STEP_KEYWORD}", stepAnnotationName).replace("${STEP_REGEXP}", "\"" + regexp + "\"")
//      .replace("${METHOD_NAME}", methodFromSnippet.getName())
//      .replace("${PARAMETERS}", methodFromSnippet.getParameterList().getText()).replace("${BODY}\n", "");
//
//    text = processGeneratedStepDefinition(text, methodFromSnippet);
//
//    return factory.createMethodFromText(text, step);
//  }
}

