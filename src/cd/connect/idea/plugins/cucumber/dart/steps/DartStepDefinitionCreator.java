package cd.connect.idea.plugins.cucumber.dart.steps;

import cd.connect.idea.plugins.cucumber.dart.steps.snippets.SnippetGenerator;
import com.intellij.codeInsight.CodeInsightUtilCore;
import com.intellij.lang.Language;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.project.Project;
import com.intellij.pom.Navigatable;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartFile;
import cucumber.runtime.snippets.CamelCaseConcatenator;
import cucumber.runtime.snippets.FunctionNameGenerator;
import gherkin.pickles.PickleStep;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;

import java.util.ArrayList;

import static com.jetbrains.lang.dart.util.DartElementGenerator.createDummyFile;

public class DartStepDefinitionCreator extends BaseDartStepDefinitionCreator {

  @Override
  public boolean createStepDefinition(@NotNull GherkinStep step, @NotNull PsiFile file, boolean withTemplate) {
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

    ((Navigatable)clazz.getClassBody().getClassMembers().getLastChild()).navigate(true);
    
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
    final PickleStep cucumberStep = new PickleStep(step.getName(), new ArrayList<>(), new ArrayList<>());
//    final Step cucumberStep = new Step(new ArrayList<>(), step.getKeyword().getText(), step.getStepName(), 0, null, null);
    final SnippetGenerator generator = new SnippetGenerator(new DartSnippet());

    String snippetTemplate = generator.getSnippet(cucumberStep, step.getKeyword().getText(), new FunctionNameGenerator(new CamelCaseConcatenator()));
    String snippet = processGeneratedStepDefinition(snippetTemplate, step);

    PsiElement expression = createMethodFromText(step.getProject(), snippet);
//    JVMElementFactory factory = JVMElementFactories.requireFactory(language, step.getProject());
//    PsiElement expression =  factory.createExpressionFromText(snippet, step);


    return expression;
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
}
