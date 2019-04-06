package org.jetbrains.plugins.cucumber.dart.steps.reference;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiMethod;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartMethodDeclaration;

import static org.jetbrains.plugins.cucumber.dart.CucumberDartUtil.isHook;
import static org.jetbrains.plugins.cucumber.dart.CucumberDartUtil.isStepDefinition;
import static org.jetbrains.plugins.cucumber.dart.CucumberDartUtil.isStepDefinitionClass;

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
