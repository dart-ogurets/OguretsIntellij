package io.featurehub.cucumber.dart;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.jetbrains.lang.dart.DartFileType;
import io.featurehub.cucumber.dart.steps.DartStepDefinitionCreator;
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
    // Gherkin plugin 261+ API. No @Override: these don't exist in the 253 API we compile against,
    // but they implement the abstract interface methods at runtime on newer IDEs.
    public boolean isStepLikeFile(@NotNull PsiElement child) {
        return CucumberDartUtil.isStepLikeFile(child);
    }

    public boolean isWritableStepLikeFile(@NotNull PsiElement child) {
        return CucumberDartUtil.isStepLikeFile(child);
    }

    // Gherkin plugin 253 API (removed in 263)
    @Override
    public boolean isStepLikeFile(@NotNull PsiElement child, @NotNull PsiElement parent) {
        return isStepLikeFile(child);
    }

    @Override
    public boolean isWritableStepLikeFile(@NotNull PsiElement child, @NotNull PsiElement parent) {
        return isWritableStepLikeFile(child);
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

    // Gherkin plugin 262+ API (no @Override, see above)
    public List<AbstractStepDefinition> loadStepsFor(@NotNull Module module) {
        return CucumberDartUtil.loadStepsFor(null, module);
    }

    // Gherkin plugin 253 API (deprecated in 262+)
    @Override
    public List<AbstractStepDefinition> loadStepsFor(@Nullable PsiFile featureFile, @NotNull Module module) {
        return CucumberDartUtil.loadStepsFor(featureFile, module);
    }

    @Override
    public Collection<? extends PsiFile> getStepDefinitionContainers(@NotNull GherkinFile featureFile) {
        return CucumberDartUtil.getStepDefinitionContainers(featureFile);
    }
}
