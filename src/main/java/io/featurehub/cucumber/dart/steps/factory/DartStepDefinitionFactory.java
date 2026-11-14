// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package io.featurehub.cucumber.dart.steps.factory;

import com.intellij.openapi.module.Module;
import com.intellij.psi.PsiElement;
import io.featurehub.cucumber.dart.steps.AbstractDartStepDefinition;
import org.jetbrains.annotations.NotNull;

public abstract class DartStepDefinitionFactory {
    public static DartStepDefinitionFactory getInstance(@NotNull Module module) {
        return new DartStep2xDefinitionFactory();
    }

    public abstract AbstractDartStepDefinition buildStepDefinition(@NotNull PsiElement element, @NotNull String annotationClassName);
}
