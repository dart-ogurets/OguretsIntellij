package dev.bluebiscuitdesign.cucumber.dart;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.lang.dart.DartFileType;
import dev.bluebiscuitdesign.cucumber.dart.steps.DartStepDefinitionCreator;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.plugins.cucumber.BDDFrameworkType;
import org.jetbrains.plugins.cucumber.StepDefinitionCreator;
import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.steps.AbstractCucumberExtension;
import org.jetbrains.plugins.cucumber.steps.AbstractStepDefinition;

import java.util.Collection;
import java.util.List;

public class CucumberDartNIExtension extends AbstractCucumberExtension {
    @Override
    public boolean isStepLikeFile(@NotNull PsiElement child, @NotNull PsiElement parent) {
        return CucumberDartUtil.isStepLikeFile(child, parent);
    }

    @Override
    public boolean isWritableStepLikeFile(@NotNull PsiElement child, @NotNull PsiElement parent) {
        return CucumberDartUtil.isWritableStepLikeFile(child, parent);
    }

    @NotNull
    @Override
    public BDDFrameworkType getStepFileType() {
        return new BDDFrameworkType(DartFileType.INSTANCE, "Dart 2");
    }

    @NotNull
    @Override
    public StepDefinitionCreator getStepDefinitionCreator() {
        return new DartStepDefinitionCreator();
    }

    @Override
    public List<AbstractStepDefinition> loadStepsFor(@Nullable PsiFile featureFile, @NotNull Module module) {
        return CucumberDartUtil.loadStepsFor(featureFile, module);
    }

    @Override
    public Collection<? extends PsiFile> getStepDefinitionContainers(@NotNull GherkinFile featureFile) {
        return CucumberDartUtil.getStepDefinitionContainers(featureFile);
    }
}
