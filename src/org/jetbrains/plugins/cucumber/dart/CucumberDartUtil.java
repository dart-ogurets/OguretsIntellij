package org.jetbrains.plugins.cucumber.dart;

import com.intellij.find.findUsages.JavaFindUsagesHelper;
import com.intellij.find.findUsages.JavaMethodFindUsagesOptions;
import com.intellij.openapi.module.Module;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.project.ProjectUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.JavaPsiFacade;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiConstantEvaluationHelper;
import com.intellij.psi.PsiDirectory;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiExpression;
import com.intellij.psi.PsiExpressionList;
import com.intellij.psi.PsiManager;
import com.intellij.psi.PsiMethod;
import com.intellij.psi.PsiNewExpression;
import com.intellij.psi.SmartPointerManager;
import com.intellij.psi.SmartPsiElementPointer;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.ClassUtil;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.usageView.UsageInfo;
import com.intellij.util.CommonProcessors;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartMetadata;
import com.jetbrains.lang.dart.psi.DartMethodDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.MapParameterTypeManager;
import org.jetbrains.plugins.cucumber.dart.steps.reference.CucumberJavaAnnotationProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.jetbrains.plugins.cucumber.CucumberUtil.STANDARD_PARAMETER_TYPES;
import static org.jetbrains.plugins.cucumber.MapParameterTypeManager.DEFAULT;

public class CucumberDartUtil {
  public static final String PARAMETER_TYPE_CLASS = "io.cucumber.cucumberexpressions.ParameterType";

  private static final Map<String, String> DART_PARAMETER_TYPES;

  private static final Pattern BEGIN_ANCHOR = Pattern.compile("^\\^.*");
  private static final Pattern END_ANCHOR = Pattern.compile(".*\\$$");
  private static final Pattern SCRIPT_STYLE_REGEXP = Pattern.compile("^/(.*)/$");
  private static final Pattern PARENTHESIS = Pattern.compile("\\(([^)]+)\\)");
  private static final Pattern ALPHA = Pattern.compile("[a-zA-Z]+");

  static {
    Map<String, String> dartParameterTypes = new HashMap<>();
    // only add the things that aren't there
    dartParameterTypes.put("double", STANDARD_PARAMETER_TYPES.get("float"));

    DART_PARAMETER_TYPES = Collections.unmodifiableMap(dartParameterTypes);
  }

  /**
   * Checks if expression should be considered as a CucumberExpression or as a RegEx
   * @see <a href="http://google.com">https://github.com/cucumber/cucumber/blob/master/cucumber-expressions/java/heuristics.adoc</a>
   */
  public static boolean isCucumberExpression(@NotNull String expression) {
    Matcher m = BEGIN_ANCHOR.matcher(expression);
    if (m.find()) {
      return false;
    }
    m = END_ANCHOR.matcher(expression);
    if (m.find()) {
      return false;
    }
    m = SCRIPT_STYLE_REGEXP.matcher(expression);
    if (m.find()) {
      return false;
    }
    m = PARENTHESIS.matcher(expression);
    if (m.find()) {
      String insideParenthesis = m.group(1);
      if (ALPHA.matcher(insideParenthesis).lookingAt()) {
        return true;
      }
      return false;
    }
    return true;
  }

  public static String getCucumberPendingExceptionFqn(@NotNull final PsiElement context) {
    return "PendingException";
  }

  public static boolean isStepDefinition(@NotNull final DartMethodDeclaration method) {
    return findDartCucumberAnnotation(method) != null;
  }

  public static boolean isHook(@NotNull final DartMethodDeclaration method) {
    return CucumberJavaAnnotationProvider.HOOK_MARKERS.contains(findDartCucumberAnnotation(method));
  }

  public static boolean isStepDefinitionClass(@NotNull final DartClassDefinition clazz) {
    return clazz.getClassBody() != null &&
      clazz.getClassBody().getClassMembers() != null &&
      clazz.getClassBody().getClassMembers().getMethodDeclarationList().stream().anyMatch(m -> findDartCucumberAnnotation(m) != null);
  }

  @Nullable
  public static String findDartAnnotationText(DartMethodDeclaration dc) {
    return dc.getMetadataList().stream().filter(meta ->
      isDartMetadataCucumberAnnotation(meta))
      .map(meta -> stripQuotes(meta.getReferenceExpression().getNextSibling().getFirstChild().getNextSibling().getText()))
      .findFirst()
      .orElse(null);
  }

  public static boolean isDartMetadataCucumberAnnotation(DartMetadata meta) {
    return CucumberJavaAnnotationProvider.HOOK_MARKERS.contains(meta.getReferenceExpression().getFirstChild().getText()) ||
      CucumberJavaAnnotationProvider.STEP_MARKERS.contains(meta.getReferenceExpression().getFirstChild().getText());
  }

  public static boolean isTextOfCucumberAnnotation(PsiElement d) {
    return d.getParent() != null && d.getParent().getParent() != null && d.getParent().getParent().getParent() != null &&
      d.getParent().getParent().getParent() instanceof DartMetadata;
  }


  protected static String stripQuotes(String str) {
    // raw string?
    if (str.startsWith("r\"") || str.startsWith("r'")) {
      str = str.substring(2);
    }
    if (str.startsWith("\"") || str.startsWith("'")) {
      str = str.substring(1);
    }
    if (str.endsWith("\"") || str.endsWith("'")) {
      str = str.substring(0, str.length() - 1);
    }

    // undo the backslash that prevents escaping in Dart
    str = str.replace("\\$", "$");

    return str;
  }

  @Nullable
  public static String findDartCucumberAnnotation(DartMethodDeclaration dc) {
    return dc.getMetadataList().stream().filter(CucumberDartUtil::isDartMetadataCucumberAnnotation)
      .map(meta -> meta.getReferenceExpression().getFirstChild().getText())
      .findFirst()
      .orElse(null);
  }

  public static MapParameterTypeManager getAllParameterTypes(@NotNull Module module) {
    Project project = module.getProject();
    PsiManager manager = PsiManager.getInstance(project);

    VirtualFile projectDir = ProjectUtil.guessProjectDir(project);
    PsiDirectory psiDirectory = projectDir != null ? manager.findDirectory(projectDir) : null;
    if (psiDirectory != null) {
      return CachedValuesManager.getCachedValue(psiDirectory, () ->
        CachedValueProvider.Result.create(doGetAllParameterTypes(module), PsiModificationTracker.MODIFICATION_COUNT));
    }

    return DEFAULT;
  }

  @NotNull
  private static MapParameterTypeManager doGetAllParameterTypes(@NotNull Module module) {
    final GlobalSearchScope dependenciesScope = module.getModuleWithDependenciesAndLibrariesScope(true);
    CommonProcessors.CollectProcessor<UsageInfo> processor = new CommonProcessors.CollectProcessor<>();
    JavaMethodFindUsagesOptions options = new JavaMethodFindUsagesOptions(dependenciesScope);

    PsiClass parameterTypeClass = ClassUtil.findPsiClass(PsiManager.getInstance(module.getProject()), PARAMETER_TYPE_CLASS);
    if (parameterTypeClass != null) {
      for (PsiMethod constructor: parameterTypeClass.getConstructors()) {
        JavaFindUsagesHelper.processElementUsages(constructor, options, processor);
      }
    }

    SmartPointerManager smartPointerManager = SmartPointerManager.getInstance(module.getProject());
    Map<String, String> values = new HashMap<>();
    Map<String, SmartPsiElementPointer<PsiElement>> declarations = new HashMap<>();
    for (UsageInfo ui: processor.getResults()) {
      PsiElement element = ui.getElement();
      if (element != null && element.getParent() instanceof PsiNewExpression) {
        PsiNewExpression newExpression = (PsiNewExpression)element.getParent();
        PsiExpressionList arguments = newExpression.getArgumentList();
        if (arguments != null) {
          PsiExpression[] expressions = arguments.getExpressions();
          if (expressions.length > 1) {
            PsiConstantEvaluationHelper evaluationHelper = JavaPsiFacade.getInstance(module.getProject()).getConstantEvaluationHelper();

            Object constantValue = evaluationHelper.computeConstantExpression(expressions[0], false);
            if (constantValue == null) {
              continue;
            }
            String name = constantValue.toString();

            constantValue = evaluationHelper.computeConstantExpression(expressions[1], false);
            if (constantValue == null) {
              continue;
            }
            String value = constantValue.toString();
            values.put(name, value);

            SmartPsiElementPointer<PsiElement> smartPointer = smartPointerManager.createSmartPsiElementPointer(expressions[0]);
            declarations.put(name, smartPointer);
          }
        }
      }
    }

    values.putAll(STANDARD_PARAMETER_TYPES);
    values.putAll(DART_PARAMETER_TYPES);
    return new MapParameterTypeManager(values, declarations);
  }

  /**
   * Checks if library with CucumberExpressions library attached to the project.
   * @return true if step definitions should be written in Cucumber Expressions (since Cucumber v 3.0),
   * false in case of old-style Regexp step definitions.
   */
  public static boolean isCucumberExpressionsAvailable(@NotNull PsiElement context) {
    return true;
  }
}
