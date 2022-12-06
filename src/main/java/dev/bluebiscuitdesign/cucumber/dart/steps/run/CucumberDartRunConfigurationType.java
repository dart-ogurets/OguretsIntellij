package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.configurations.ConfigurationFactory;
import com.intellij.execution.configurations.ConfigurationTypeBase;
import com.intellij.execution.configurations.RunConfiguration;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.NotNullLazyValue;
import com.intellij.psi.search.FileTypeIndex;
import com.intellij.psi.search.GlobalSearchScope;
import com.jetbrains.lang.dart.DartFileType;
import icons.CucumberJavaIcons;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;

public class CucumberDartRunConfigurationType extends ConfigurationTypeBase {
  public CucumberDartRunConfigurationType() {
    super("CucumberDartRunConfigurationType", "Ogurets (Cucumber Dart)", null,
      NotNullLazyValue.createValue(() -> CucumberJavaIcons.DartTest));
    addFactory(new DartTestConfigurationFactory(this));
  }

  @Override
  public String getHelpTopic() {
    return "reference.dialogs.rundebug.DartTestRunConfigurationType";
  }

  public static class DartTestConfigurationFactory extends ConfigurationFactory {
    protected DartTestConfigurationFactory(CucumberDartRunConfigurationType type) {
      super(type);
    }

    @Override
    public @NotNull
    @NonNls String getId() {
      return "Ogurets (Cucumber Dart)";
    }

    @Override
    @NotNull
    public RunConfiguration createTemplateConfiguration(@NotNull Project project) {
      return new CucumberDartRunConfiguration(project, this, "Dart");
    }

    @Override
    public boolean isApplicable(@NotNull Project project) {
      return FileTypeIndex.containsFileOfType(DartFileType.INSTANCE, GlobalSearchScope.projectScope(project));
    }
  }
}
