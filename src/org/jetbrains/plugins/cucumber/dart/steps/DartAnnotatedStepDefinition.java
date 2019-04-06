package org.jetbrains.plugins.cucumber.dart.steps;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.jetbrains.lang.dart.psi.DartMethodDeclaration;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.dart.CucumberDartUtil;

public class DartAnnotatedStepDefinition extends AbstractDartStepDefinition {
  private final String myAnnotationClassName;

  public DartAnnotatedStepDefinition(@NotNull PsiElement stepDef, @NotNull String annotationClassName) {
    super(stepDef);
    myAnnotationClassName = annotationClassName;
  }

  @Nullable
  @Override
  protected String getCucumberRegexFromElement(PsiElement element) {
    if (element == null) {
      return null;
    }

    if (!(element instanceof DartMethodDeclaration)) {
      return null;
    }

    return CucumberDartUtil.findDartAnnotationText((DartMethodDeclaration)element);
  }
}
