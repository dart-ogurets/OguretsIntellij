package dev.bluebiscuitdesign.cucumber.dart.steps;

import com.intellij.psi.PsiElement;
import com.jetbrains.lang.dart.psi.DartComponent;
import dev.bluebiscuitdesign.cucumber.dart.CucumberDartUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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
        if (!CucumberDartUtil.isStepDefinitionOwner(element)) {
            return null;
        }
        return CucumberDartUtil.findDartAnnotationText((DartComponent) element);
    }
}
