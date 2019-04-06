package org.jetbrains.plugins.cucumber.dart.steps;

import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateDescriptor;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.psi.JVMElementFactories;
import com.intellij.psi.JVMElementFactory;
import com.intellij.psi.JavaTokenType;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiCodeBlock;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiLambdaExpression;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiMethodCallExpression;
import com.intellij.psi.PsiParameterList;
import com.intellij.psi.TokenType;
import com.intellij.psi.codeStyle.JavaCodeStyleManager;
import com.intellij.psi.impl.source.tree.Factory;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartFile;
import com.jetbrains.lang.dart.psi.DartFunctionBody;
import com.jetbrains.lang.dart.psi.DartFunctionDeclarationWithBodyOrNative;
import com.jetbrains.lang.dart.psi.DartStatements;
import com.jetbrains.lang.dart.psi.IDartBlock;
import com.jetbrains.lang.dart.util.DartElementGenerator;
import cucumber.runtime.snippets.CamelCaseConcatenator;
import cucumber.runtime.snippets.FunctionNameGenerator;
import cucumber.runtime.snippets.SnippetGenerator;
import gherkin.pickles.PickleStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;

import java.util.ArrayList;

import static com.jetbrains.lang.dart.util.DartElementGenerator.createDummyFile;

public class DartStepDefinitionCreator extends BaseDartStepDefinitionCreator {
  public static final String CUCUMBER_API_JAVA8_EN = "cucumber.api.dart2.En";
  private static final String FILE_TEMPLATE_CUCUMBER_JAVA_8_STEP_DEFINITION_JAVA = "Cucumber Dart Step Definition.dart";

  @Override
  public boolean createStepDefinition(@NotNull GherkinStep step, @NotNull PsiFile file) {
    if (!(file instanceof DartFile)) return false;

    final DartClassDefinition clazz = PsiTreeUtil.getChildOfType(file, DartClassDefinition.class);
    if (clazz == null) {
      return false;
    }

    final Project project = file.getProject();
    closeActiveTemplateBuilders(file);
    PsiDocumentManager.getInstance(project).commitAllDocuments();

    final PsiElement stepDef = buildStepDefinitionByStep(step, file.getLanguage());

    PsiElement addedStepDef = clazz.getClassBody().getClassMembers().add(stepDef);

//    final PsiMethod constructor = getConstructor(clazz);
//    final PsiCodeBlock constructorBody = constructor.getBody();
//    if (constructorBody == null) {
//      return false;
//    }
//
//    PsiElement anchor = constructorBody.getFirstChild();
//    if (constructorBody.getStatements().length > 0) {
//      anchor = constructorBody.getStatements()[constructorBody.getStatements().length - 1];
//    }
//    PsiElement addedStepDef = constructorBody.addAfter(stepDef, anchor);

    addedStepDef = CodeInsightUtilCore.forcePsiPostprocessAndRestoreElement(addedStepDef);
//
//    JavaCodeStyleManager.getInstance(project).shortenClassReferences(addedStepDef);
//
    Editor editor = FileEditorManager.getInstance(project).getSelectedTextEditor();
    assert editor != null;
//
//    if (!(addedStepDef instanceof PsiMethodCallExpression)) {
//      return false;
//    }
//    PsiMethodCallExpression stepDefCall = (PsiMethodCallExpression)addedStepDef;
//    if (stepDefCall.getArgumentList().getExpressions().length < 2) {
//      return false;
//    }
//
//    final PsiExpression regexpElement = stepDefCall.getArgumentList().getExpressions()[0];
//
//    final PsiExpression secondArgument = stepDefCall.getArgumentList().getExpressions()[1];
//    if (!(secondArgument instanceof PsiLambdaExpression)) {
//      return false;
//    }
//    PsiLambdaExpression lambda = (PsiLambdaExpression)secondArgument;
//    final PsiParameterList blockVars = lambda.getParameterList();
//    PsiElement lambdaBody = lambda.getBody();
//    if (!(lambdaBody instanceof PsiCodeBlock)) {
//      return false;
//    }
//    final PsiCodeBlock body = (PsiCodeBlock)lambdaBody;
//
//    runTemplateBuilderOnAddedStep(editor, addedStepDef, regexpElement, blockVars, body);

    return true;
  }

  private static PsiElement buildStepDefinitionByStep(@NotNull final GherkinStep step, Language language) {
    final PickleStep cucumberStep = new PickleStep(step.getStepName(), new ArrayList<>(), new ArrayList<>());
//    final Step cucumberStep = new Step(new ArrayList<>(), step.getKeyword().getText(), step.getStepName(), 0, null, null);
    final SnippetGenerator generator = new SnippetGenerator(new DartSnippet());

    String snippetTemplate = generator.getSnippet(cucumberStep, step.getKeyword().getText(), new FunctionNameGenerator(new CamelCaseConcatenator()));
    String snippet = processGeneratedStepDefinition(snippetTemplate, step);

    PsiElement expression = createMethodFromText(step.getProject(), snippet);
//    JVMElementFactory factory = JVMElementFactories.requireFactory(language, step.getProject());
//    PsiElement expression =  factory.createExpressionFromText(snippet, step);

    try {
      return createStepDefinitionFromSnippet(expression, step);
    } catch (Exception e) {
      return expression;
    }
  }

  @Nullable
  public static PsiElement createMethodFromText(Project myProject, String text) {
    final PsiFile file = createDummyFile(myProject, text);
    final PsiElement child = file.getFirstChild();
//    if (child instanceof DartFunctionDeclarationWithBodyOrNative) {
//      final DartFunctionBody functionBody = ((DartFunctionDeclarationWithBodyOrNative)child).getFunctionBody();
//      final IDartBlock block = PsiTreeUtil.getChildOfType(functionBody, IDartBlock.class);
//      final DartStatements statements = block == null ? null : block.getStatements();
//      return statements == null ? null : statements.getFirstChild();
//    }
    return child;
  }

  private static PsiElement createStepDefinitionFromSnippet(@NotNull PsiElement snippetExpression, @NotNull GherkinStep step) {
    PsiMethodCallExpression callExpression = (PsiMethodCallExpression)snippetExpression;
    PsiExpression[] arguments = callExpression.getArgumentList().getExpressions();
    PsiLambdaExpression lambda = (PsiLambdaExpression)arguments[1];

    FileTemplateDescriptor fileTemplateDescriptor = new FileTemplateDescriptor(FILE_TEMPLATE_CUCUMBER_JAVA_8_STEP_DEFINITION_JAVA);
    FileTemplate fileTemplate = FileTemplateManager.getInstance(snippetExpression.getProject()).getCodeTemplate(fileTemplateDescriptor.getFileName());
    String text = fileTemplate.getText().replace("${STEP_KEYWORD}", callExpression.getMethodExpression().getText())
      .replace("${STEP_REGEXP}", arguments[0].getText())
      .replace("${PARAMETERS}", lambda.getParameterList().getText())
      .replace("${BODY}\n", "");

    text = processGeneratedStepDefinition(text, snippetExpression);

    return DartElementGenerator.createExpressionFromText(snippetExpression.getProject(), text);
//    return factory.createExpressionFromText(text, step);
  }
}
