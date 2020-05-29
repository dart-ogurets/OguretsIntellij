// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.bluebiscuitdesign.cucumber.dart.steps.factory;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import dev.bluebiscuitdesign.cucumber.dart.steps.AbstractDartStepDefinition;
import dev.bluebiscuitdesign.cucumber.dart.steps.DartStep2XDefinition;

public class DartStep2xDefinitionFactory extends DartStepDefinitionFactory {
  @Override
  public AbstractDartStepDefinition buildStepDefinition(@NotNull PsiElement element, @NotNull String annotationClassName) {
    return new DartStep2XDefinition(element, annotationClassName);
  }
}
