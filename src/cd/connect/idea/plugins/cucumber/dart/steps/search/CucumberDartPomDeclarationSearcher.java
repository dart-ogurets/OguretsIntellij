// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package cd.connect.idea.plugins.cucumber.dart.steps.search;

import com.intellij.pom.PomDeclarationSearcher;
import com.intellij.pom.PomTarget;
import com.intellij.psi.PsiElement;
import com.intellij.util.Consumer;
import org.jetbrains.annotations.NotNull;

public class CucumberDartPomDeclarationSearcher extends PomDeclarationSearcher {
  @Override
  public void findDeclarationsAt(@NotNull PsiElement element, int offsetInElement, Consumer<PomTarget> consumer) {

    // i can't see this being useful because annotations are regex's and this search isn't searching by regex but by
    // text?

//    if (!(element instanceof DartStringLiteralExpression)) {
//      if (CucumberDartUtil.isTextOfCucumberAnnotation(element))
//    }

//    consumer.consume(new CucumberJavaParameterPomTarget(element, element.getText()));

//    if (!(element instanceof PsiLiteralExpression)) {
//      return;
//    }
//
//    Object value = ((PsiLiteralExpression)element).getValue();
//    if (!(value instanceof String)) {
//      return;
//    }
//    String stringValue = (String)value;
//
//    PsiNewExpression newExp = PsiTreeUtil.getParentOfType(element, PsiNewExpression.class);
//    if (newExp != null) {
//      if (!isFirstConstructorArgument(element, newExp)) {
//        return;
//      }
//      PsiJavaCodeReferenceElement classReference = newExp.getClassReference();
//      if (classReference != null) {
//        String fqn = classReference.getQualifiedName();
//        if (CucumberDartUtil.PARAMETER_TYPE_CLASS.equals(fqn)) {
//          consumer.consume(new CucumberJavaParameterPomTarget(element, stringValue));
//        }
//      }
//    }
  }

//  private static boolean isFirstConstructorArgument(@NotNull PsiElement element, @NotNull PsiNewExpression newExp) {
//    PsiExpressionList argumentList = newExp.getArgumentList();
//    if (argumentList == null) {
//      return false;
//    }
//
//    if (argumentList.getExpressionCount() == 0) {
//      return false;
//    }
//
//    if (argumentList.getExpressions()[0] != element) {
//      return false;
//    }
//    return true;
//  }
}
