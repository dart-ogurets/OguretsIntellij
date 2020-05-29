package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.openapi.project.Project;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineRunnerParameters;
import com.jetbrains.lang.dart.sdk.DartConfigurable;
import com.jetbrains.lang.dart.sdk.DartSdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class CucumberDartRunnerParameters extends DartCommandLineRunnerParameters implements Cloneable {
  @NotNull
  private Scope cucumberScope = Scope.FEATURE;
  @Nullable private String testRunnerOptions = null;
  @Nullable private String nameFilter = null;
  @Nullable private String cucumberFilePath = null;
  @Nullable private String dartFilePath = null;
  private boolean flutterEnabled = false;
  private TestType testType;
  @Nullable
  private String flutterObservatoryUrl;
  @Nullable
  private String buildFlavour;
  @Nullable
  private String deviceId;

  @Nullable
  public String getBuildFlavour() {
    return buildFlavour;
  }

  public void setBuildFlavour(@Nullable String buildFlavour) {
    this.buildFlavour = buildFlavour;
  }

  @Nullable
  public String getDeviceId() {
    return deviceId;
  }

  public void setDeviceId(@Nullable String deviceId) {
    this.deviceId = deviceId;
  }

  @NotNull
  public Scope getCucumberScope() {
    return cucumberScope;
  }

  public void setCucumberScope(final Scope scope) {
    if (scope != null) { // null in case of corrupted storage
      cucumberScope = scope;
    }
  }

  @Nullable
  public String getFlutterObservatoryUrl() {
    return flutterObservatoryUrl;
  }

  public void setFlutterObservatoryUrl(@Nullable String flutterObservatoryUrl) {
    this.flutterObservatoryUrl = flutterObservatoryUrl;
  }

  @Nullable
  public String getCucumberFilePath() {
    return cucumberFilePath;
  }

  public void setCucumberFilePath(@Nullable String cucumberFilePath) {
    this.cucumberFilePath = cucumberFilePath;
  }

  @Nullable
  public String getTestRunnerOptions() {
    return testRunnerOptions;
  }

  @Nullable
  public String getDartFilePath() {
    return dartFilePath;
  }
  
  public void setDartFilePath(@Nullable String dartFilePath) {
    setFilePath(dartFilePath);
    this.dartFilePath = dartFilePath;
  }

  public void setTestRunnerOptions(@Nullable String testRunnerOptions) {
    this.testRunnerOptions = testRunnerOptions;
  }

  @Nullable
  public String getNameFilter() {
    return nameFilter;
  }

  public void setNameFilter(@Nullable String nameFilter) {
    this.nameFilter = nameFilter;
  }

  public boolean isFlutterEnabled() {
    return flutterEnabled;
  }

  public void setFlutterEnabled(boolean flutterEnabled) {
    this.flutterEnabled = flutterEnabled;
  }

  public TestType getTestType() {
    return testType;
  }

  public void setTestType(TestType testType) {
    this.testType = testType;
  }

  // how do i make this a property that is not persisted?
  public static boolean isFlutterDriverExecutable(CucumberDartRunnerParameters runnerParameters) {
    return runnerParameters.getDartFilePath() != null &&
      !runnerParameters.getDartFilePath().endsWith("_test.dart") &&
      runnerParameters.getDartFilePath().contains("test_driver");
  }

  public enum Scope {
    FOLDER("All in folder"),
    FEATURE("All scenarios in feature file"),
    SCENARIO("A scenario"); // Used by test re-runner action; not visible in UI

    private final String myPresentableName;

    Scope(final String name) {
      myPresentableName = name;
    }

    public String getPresentableName() {
      return myPresentableName;
    }
  }

  @Override
  protected CucumberDartRunnerParameters clone() {
    CucumberDartRunnerParameters p = (CucumberDartRunnerParameters)super.clone();

    CucumberDartRunnerParameters myRunnerParameters = this;

    Map<String, String> env  = p.getEnvs();
    env.remove("CUCUMBER_FOLDER");
    env.remove("CUCUMBER_FEATURE");
    env.remove("CUCUMBER");
    env.remove("CUCUMBER_SCENARIO");

    if (myRunnerParameters.getCucumberScope() == CucumberDartRunnerParameters.Scope.FOLDER) {
      env.put("CUCUMBER_FOLDER", myRunnerParameters.getCucumberFilePath());
      env.put("CUCUMBER", "FOLDER");
    } else if (myRunnerParameters.getCucumberScope() == CucumberDartRunnerParameters.Scope.FEATURE) {
      if (myRunnerParameters.getCucumberFilePath() != null) {
        env.put("CUCUMBER_FEATURE", myRunnerParameters.getCucumberFilePath());
      } else {
        env.remove("CUCUMBER_FEATURE");
      }
      env.put("CUCUMBER", "FEATURE");
    } else { // if (myRunnerParameters.getCucumberScope() == CucumberDartRunnerParameters.Scope.SCENARIO) {
      if (myRunnerParameters.getCucumberFilePath() != null) {
        env.put("CUCUMBER_FEATURE", myRunnerParameters.getCucumberFilePath());
      } else {
        env.remove("CUCUMBER_FEATURE");
      }
      env.put("CUCUMBER_SCENARIO", myRunnerParameters.getNameFilter());
      env.put("CUCUMBER", "SCENARIO");
    }

    // we have flutter, we have an observatory url, and its a test in the integration folder
    if (flutterEnabled && flutterObservatoryUrl != null && flutterObservatoryUrl.length() > 0 && testType == TestType.Integration) {
      env.put("VM_SERVICE_URL", flutterObservatoryUrl);
    } else {
      env.remove("VM_SERVICE_URL");
    }
    
    p.setDeviceId(getDeviceId());
    p.setBuildFlavour(getBuildFlavour());

    if (p.getBuildFlavour() != null) {
      env.put("OGURETS_FLUTTER_FLAVOUR", p.getBuildFlavour());
    } else {
      env.remove("OGURETS_FLUTTER_FLAVOUR");
    }

    if (p.getDeviceId() != null) {
      env.put("OGURETS_FLUTTER_DEVICE_ID", p.getDeviceId());
    } else {
      env.remove("OGURETS_FLUTTER_DEVICE_ID");
    }

    if (p.getTestRunnerOptions() != null) {
      env.put("OGURETS_ADDITIONAL_ARGUMENTS", getTestRunnerOptions());
    } else {
      env.remove("OGURETS_ADDITIONAL_ARGUMENTS");
    }

    return p;
  }

  // difference important because of which directory they are in and what kind of runner we use.
  public enum TestType {
    Test, Integration
  }

  // this is checking to ensure that we are a valid run config
  @Override
  public void check(@NotNull Project project) throws RuntimeConfigurationError {
    try {
      DartSdk sdk = DartSdk.getDartSdk(project);
      if (sdk == null) {
        throw new RuntimeConfigurationError(DartBundle.message("dart.sdk.is.not.configured", new Object[0]), () -> {
          DartConfigurable.openDartSettings(project);
        });
      }
    } catch (Exception e) {
      e.printStackTrace();
      throw new RuntimeConfigurationError(DartBundle.message("dart.sdk.is.not.configured", new Object[0]), () -> {
        DartConfigurable.openDartSettings(project);
      });
    }
  }

}
