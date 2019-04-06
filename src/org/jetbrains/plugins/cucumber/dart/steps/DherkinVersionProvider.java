package org.jetbrains.plugins.cucumber.dart.steps;

import org.jetbrains.plugins.cucumber.psi.GherkinStep;

public class DherkinVersionProvider {
  public String getVersion(GherkinStep step) {
    return "3.0";
  }
}