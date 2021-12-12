// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package dev.bluebiscuitdesign.cucumber.dart.steps.run.ui;

import com.intellij.uiDesigner.core.GridConstraints;
import com.intellij.uiDesigner.core.GridLayoutManager;
import com.intellij.uiDesigner.core.Spacer;
import dev.bluebiscuitdesign.cucumber.dart.steps.run.CucumberDartRunConfiguration;
import dev.bluebiscuitdesign.cucumber.dart.steps.run.CucumberDartRunnerParameters;
import com.intellij.execution.configuration.EnvironmentVariablesComponent;
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory;
import com.intellij.openapi.options.ConfigurationException;
import com.intellij.openapi.options.SettingsEditor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.TextFieldWithBrowseButton;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.util.registry.Registry;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.ide.runner.server.ui.DartCommandLineConfigurationEditorForm;
import com.jetbrains.lang.dart.util.PubspecYamlUtil;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;

public class CucumberDartConfigurationEditorForm extends SettingsEditor<CucumberDartRunConfiguration> {

  private JPanel myMainPanel;
  private JLabel myScenario;
  private JLabel myTestFileLabel;
  private TextFieldWithBrowseButton myFileField;
  private JLabel myDirLabel;
  private TextFieldWithBrowseButton myDirField;
  private JLabel myOguretsMainFileNameLabel;
  private JLabel observatoryUrlLabel;
  private JTextField myDartFileNameField;
  private JTextField myFlutterOptionsField;
  private EnvironmentVariablesComponent myEnvironmentVariables;
  private TextFieldWithBrowseButton myDartFile;
  private JLabel scenarioLabel;
  private JTextField txtObservatoryUrl;
  private JLabel jOgurets;
  private JLabel cucumberIcon;
  private JTextField myBuildFlavour;
  private JLabel lblBuildFlavour;
  private JTextField myDeviceId;
  private CucumberDartRunnerParameters.Scope scope;
  private boolean flutterEnabled;

  public CucumberDartConfigurationEditorForm(@NotNull final Project project) {
    try {


      // show how to select the dart files
      DartCommandLineConfigurationEditorForm.initDartFileTextWithBrowse(project, myDartFile);

      myDartFile.addBrowseFolderListener(DartBundle.message("choose.dart.directory"), null, project,
        // Unfortunately, withFileFilter() only works for files, not directories.
        FileChooserDescriptorFactory.createSingleFolderDescriptor());
      myDartFile.addActionListener(e -> onTestDirChanged(project));

      final DocumentListener dirListener = new DocumentListener() {
        @Override
        public void insertUpdate(DocumentEvent e) {

        }

        @Override
        public void removeUpdate(DocumentEvent e) {

        }

        @Override
        public void changedUpdate(DocumentEvent e) {
          onTestDirChanged(project);
        }
      };

      myDirField.getTextField().getDocument().addDocumentListener(dirListener);
      myDirField.addBrowseFolderListener("Choose feature dir", null, project,
        // Unfortunately, withFileFilter() only works for files, not directories.
        FileChooserDescriptorFactory.createSingleFolderDescriptor());

      myFileField.addBrowseFolderListener("Choose feature file", null, project,
        // Unfortunately, withFileFilter() only works for files, not directories.
        FileChooserDescriptorFactory.createSingleFileDescriptor("feature"));

      // 'Environment variables' is the widest label, anchored by myTestFileLabel
      myTestFileLabel.setPreferredSize(myEnvironmentVariables.getLabel().getPreferredSize());
      myDirLabel.setPreferredSize(myEnvironmentVariables.getLabel().getPreferredSize());
      myEnvironmentVariables.setAnchor(myTestFileLabel);
    } catch (Throwable t) {
      t.printStackTrace();
      throw t;
    }
  }

  @Override
  protected void resetEditorFrom(@NotNull final CucumberDartRunConfiguration configuration) {
    final CucumberDartRunnerParameters parameters = configuration.getRunnerParameters();

    if (parameters.getCucumberScope() == CucumberDartRunnerParameters.Scope.SCENARIO) {
      myScenario.setText(parameters.getNameFilter());
    }

    // what is the cucumber file we are using?
    String cukeFilePath = FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getCucumberFilePath()));
    if (parameters.getCucumberScope() == CucumberDartRunnerParameters.Scope.FOLDER) {
      myDirField.setText(cukeFilePath);
    } else {
      myFileField.setText(cukeFilePath);
    }

    myDartFile.setText(parameters.getDartFilePath() == null ? "" : parameters.getDartFilePath());

//    myDartFileNameField.setText(
//      parameters.getCucumberScope() != FOLDER ? StringUtil.notNullize(parameters.getNameFilter()) : "");
    myFlutterOptionsField.setText(parameters.getTestRunnerOptions());
    myEnvironmentVariables.setEnvs(parameters.getEnvs());
    myEnvironmentVariables.setPassParentEnvs(parameters.isIncludeParentEnvs());
    txtObservatoryUrl.setText(parameters.getFlutterObservatoryUrl() == null ? "" : parameters.getFlutterObservatoryUrl());

    myDeviceId.setText(parameters.getDeviceId() == null ? "" : parameters.getDeviceId());
    myBuildFlavour.setText(parameters.getBuildFlavour() == null ? "" : parameters.getBuildFlavour());

    flutterEnabled = configuration.getRunnerParameters().isFlutterEnabled();

    scope = parameters.getCucumberScope();

    onScopeChanged();
  }

  @Override
  protected void applyEditorTo(@NotNull final CucumberDartRunConfiguration configuration) throws ConfigurationException {
    final CucumberDartRunnerParameters parameters = configuration.getRunnerParameters();

    parameters.setCucumberScope(scope);
    TextFieldWithBrowseButton pathSource = scope == CucumberDartRunnerParameters.Scope.FOLDER ? myDirField : myFileField;
    parameters.setDartFilePath(StringUtil.nullize(FileUtil.toSystemIndependentName(myDartFile.getText().trim())));
    parameters.setCucumberFilePath(StringUtil.nullize(FileUtil.toSystemIndependentName(pathSource.getText().trim())));
    parameters.setTestRunnerOptions(StringUtil.nullize(myFlutterOptionsField.getText().trim()));
    parameters.setEnvs(myEnvironmentVariables.getEnvs());
    parameters.setIncludeParentEnvs(myEnvironmentVariables.isPassParentEnvs());
    String buildFlavour = myBuildFlavour.getText().trim();
    parameters.setBuildFlavour(buildFlavour.length() == 0 ? null : buildFlavour);
    String deviceId = myDeviceId.getText().trim();
    parameters.setDeviceId(deviceId.length() == 0 ? null : deviceId);
    String url = txtObservatoryUrl.getText().trim();
    parameters.setFlutterObservatoryUrl(url.length() > 0 ? url : null);
  }

  private void onScopeChanged() {
    boolean folderMode = scope == CucumberDartRunnerParameters.Scope.FOLDER;
    boolean projectWithoutPubspec = Registry.is("dart.projects.without.pubspec", false);
    myFileField.setVisible(!folderMode);
    myTestFileLabel.setVisible(!folderMode);
    myDirField.setVisible(folderMode);
    myDirLabel.setVisible(folderMode);
    myScenario.setVisible(scope == CucumberDartRunnerParameters.Scope.SCENARIO);
    scenarioLabel.setVisible(scope == CucumberDartRunnerParameters.Scope.SCENARIO);
    txtObservatoryUrl.setEnabled(flutterEnabled);
    myFlutterOptionsField.setEnabled(flutterEnabled);
    myBuildFlavour.setEnabled(flutterEnabled);
    myDeviceId.setEnabled(flutterEnabled);
  }

  private void onTestDirChanged(Project project) {
    if (!isDirApplicable(myDirField.getText(), project)) {
      myDirField.getTextField().setForeground(Color.RED);
      final String message = DartBundle.message("test.dir.not.in.project");
      myDirField.getTextField().setToolTipText(message);
    } else {
      myDirField.getTextField().setForeground(Color.WHITE);
      myDirField.getTextField().setToolTipText(null);
    }
  }

  @NotNull
  @Override
  protected JComponent createEditor() {
    return myMainPanel;
  }

  private static boolean isDirApplicable(@NotNull final String path, @NotNull final Project project) {
    final VirtualFile file = LocalFileSystem.getInstance().findFileByPath(path);
    return file != null && file.isDirectory() && PubspecYamlUtil.findPubspecYamlFile(project, file) != null;
  }

}
