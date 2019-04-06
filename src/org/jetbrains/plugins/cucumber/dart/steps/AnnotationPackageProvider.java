package org.jetbrains.plugins.cucumber.dart.steps;

import org.jetbrains.plugins.cucumber.psi.GherkinFile;
import org.jetbrains.plugins.cucumber.psi.GherkinStep;

import static java.lang.String.format;

public class AnnotationPackageProvider {
  public AnnotationPackageProvider() {
    this(new DherkinVersionProvider());
  }

  public AnnotationPackageProvider(DherkinVersionProvider dherkinVersionProvider) {
    myVersionProvider = dherkinVersionProvider;
  }

  public String getAnnotationPackageFor(GherkinStep step) {
    return format("%s.%s", annotationBasePackage(step), locale(step));
  }

  private static String locale(GherkinStep step) {
    GherkinFile file = (GherkinFile)step.getContainingFile();
    return file.getLocaleLanguage().replaceAll("-", "_");
  }

  private final DherkinVersionProvider myVersionProvider;

  private String annotationBasePackage(GherkinStep step) {
    final String version = myVersionProvider.getVersion(step);
    return version;
  }
}