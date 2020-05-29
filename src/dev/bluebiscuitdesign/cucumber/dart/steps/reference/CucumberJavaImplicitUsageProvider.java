package dev.bluebiscuitdesign.cucumber.dart.steps.reference;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartMethodDeclaration;

import static dev.bluebiscuitdesign.cucumber.dart.CucumberDartUtil.isStepDefinition;
import static dev.bluebiscuitdesign.cucumber.dart.CucumberDartUtil.isStepDefinitionClass;

public class CucumberJavaImplicitUsageProvider implements ImplicitUsageProvider {
  @Override
  public boolean isImplicitUsage(PsiElement element) {
    if(element instanceof DartMethodDeclaration) {
      return isStepDefinition((DartMethodDeclaration)element);
    } else if (element instanceof DartClassDefinition) {
      return isStepDefinitionClass((DartClassDefinition)element);
    }

    return false;
  }

  @Override
  public boolean isImplicitRead(PsiElement element) {
    return false;
  }

  @Override
  public boolean isImplicitWrite(PsiElement element) {
    return false;
  }
}
