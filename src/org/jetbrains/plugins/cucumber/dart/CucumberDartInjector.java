package org.jetbrains.plugins.cucumber.dart;

import com.intellij.lang.Language;
import com.intellij.lang.injection.MultiHostInjector;
import com.intellij.lang.injection.MultiHostRegistrar;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.jetbrains.lang.dart.psi.DartMetadata;
import com.jetbrains.lang.dart.psi.DartStringLiteralExpression;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import com.intellij.openapi.diagnostic.Logger;


/**
 * This is intended to tell IDEA that the Given/When/etc tags are regexes.
 */

public class CucumberDartInjector implements MultiHostInjector {
  private static Logger log = Logger.getInstance(CucumberDartInjector.class);
  public static final Language regexpLanguage = Language.findLanguageByID("RegExp");

  @Override
  public void getLanguagesToInject(@NotNull MultiHostRegistrar registrar, @NotNull PsiElement element) {
    if (regexpLanguage == null) {
      log.error("unable to find regexp language id");
      return;
    }

    if (element instanceof DartStringLiteralExpression) {
      DartStringLiteralExpression d = (DartStringLiteralExpression)element;

      // ensure we are a string literal inside a support annotation and we aren't using cucumber 3 style expressions
      if (d.getText() != null && !d.getText().contains("{") && d.getParent() != null && d.getParent().getParent() != null && d.getParent().getParent().getParent() != null &&
         d.getParent().getParent().getParent() instanceof DartMetadata) {
        DartMetadata meta = (DartMetadata)d.getParent().getParent().getParent();
        if (CucumberDartUtil.isDartMetadataCucumberAnnotation(meta)) {
          int rangeStart = (element.getText().startsWith("r")) ? 2 : 1; // raw
          final TextRange range = new TextRange(rangeStart, element.getTextLength() - 1);
          registrar.startInjecting(regexpLanguage).addPlace(null, null, (PsiLanguageInjectionHost)element, range).doneInjecting();
        }
      }
    }

    // ((DartMetadataImpl) d.getParent().getParent().getParent()).getReferenceExpression()
//    if (element instanceof PsiLiteralExpression && element instanceof PsiLanguageInjectionHost && element.getTextLength() > 2) {
//      final PsiElement firstChild = element.getFirstChild();
//      if (firstChild != null && firstChild.getNode().getElementType() == JavaTokenType.STRING_LITERAL) {
//        PsiAnnotation annotation = PsiTreeUtil.getParentOfType(element, PsiAnnotation.class);
//        if (annotation != null &&
//            (CucumberDartUtil.isCucumberStepAnnotation(annotation) || CucumberDartUtil.isCucumberHookAnnotation(annotation))) {
//          final TextRange range = new TextRange(1, element.getTextLength() - 1);
//          registrar.startInjecting(regexpLanguage).addPlace(null, null, (PsiLanguageInjectionHost)element, range).doneInjecting();
//        }
//      }
//    }
  }

  @NotNull
  @Override
  public List<Class<? extends PsiElement>> elementsToInjectIn() {
    return Collections.singletonList(DartStringLiteralExpression.class);
  }
}
