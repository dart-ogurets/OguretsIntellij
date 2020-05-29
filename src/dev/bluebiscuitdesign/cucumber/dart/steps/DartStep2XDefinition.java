// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.bluebiscuitdesign.cucumber.dart.steps;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

public class DartStep2XDefinition extends DartAnnotatedStepDefinition {
  public DartStep2XDefinition(@NotNull PsiElement element, @NotNull String annotationClassName) {
    super(element, annotationClassName);
  }
}
