package org.jetbrains.plugins.cucumber.dart;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import com.intellij.openapi.diagnostic.Logger;

public class CucumberDartInjector implements MultiHostInjector {
  private static Logger log = Logger.getInstance(CucumberDartInjector.class);
  public static final Language regexpLanguage = Language.findLanguageByID("RegExp");

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement element) {
    if (regexpLanguage == null) {
      log.error("unable to find regexp language id");
      return;
    }
    if (element instanceof PsiLiteralExpression && element instanceof PsiLanguageInjectionHost && element.getTextLength() > 2) {
      final PsiElement firstChild = element.getFirstChild();
      if (firstChild != null && firstChild.getNode().getElementType() == JavaTokenType.STRING_LITERAL) {
        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
        if (annotation != null &&
            (CucumberDartUtil.isCucumberStepAnnotation(annotation) || CucumberDartUtil.isCucumberHookAnnotation(annotation))) {
          final TextRange range = new TextRange(1, element.getTextLength() - 1);
          registrar.startInjecting(regexpLanguage).addPlace(null, null, (PsiLanguageInjectionHost)element, range).doneInjecting();
        }
      }
    }
  }

  @NotNull
  @Override
  public List<Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(PsiLiteralExpression.class);
  }
}
