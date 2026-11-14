package io.featurehub.cucumber.dart.steps.reference;

import com.intellij.codeInsight.daemon.ImplicitUsageProvider;
import com.intellij.psi.PsiElement;
import com.jetbrains.lang.dart.psi.DartClassDefinition;
import com.jetbrains.lang.dart.psi.DartComponent;

import static io.featurehub.cucumber.dart.CucumberDartUtil.isStepDefinition;
import static io.featurehub.cucumber.dart.CucumberDartUtil.isStepDefinitionClass;
import static io.featurehub.cucumber.dart.CucumberDartUtil.isStepDefinitionOwner;

public class CucumberJavaImplicitUsageProvider implements ImplicitUsageProvider {
    @Override
    public boolean isImplicitUsage(PsiElement element) {
        if (isStepDefinitionOwner(element)) {
            return isStepDefinition((DartComponent) element);
        } else if (element instanceof DartClassDefinition) {
            return isStepDefinitionClass((DartClassDefinition) element);
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
