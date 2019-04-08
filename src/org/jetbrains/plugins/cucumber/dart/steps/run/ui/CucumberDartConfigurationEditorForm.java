// Copyright 2000-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE file.
package org.jetbrains.plugins.cucumber.dart.steps.run.ui;

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
import com.intellij.ui.DocumentAdapter;
import com.intellij.ui.JBColor;
import com.intellij.ui.ListCellRendererWrapper;
import com.intellij.util.ui.UIUtil;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.ide.runner.server.ui.DartCommandLineConfigurationEditorForm;
import com.jetbrains.lang.dart.util.PubspecYamlUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.cucumber.dart.steps.run.CucumberDartRunConfiguration;
import org.jetbrains.plugins.cucumber.dart.steps.run.CucumberDartRunnerParameters;

import javax.swing.*;
import javax.swing.event.DocumentEvent;

import static org.jetbrains.plugins.cucumber.dart.steps.run.CucumberDartRunnerParameters.Scope.*;

public class CucumberDartConfigurationEditorForm extends SettingsEditor<CucumberDartRunConfiguration> {

  private JPanel myMainPanel;
  private JComboBox myScenario;
  private JLabel myTestFileLabel;
  private TextFieldWithBrowseButton myFileField;
  private JLabel myDirLabel;
  private TextFieldWithBrowseButton myDirField;
  private JLabel myDherkinMainFileNameLabel;
  private JTextField myDartFileNameField;
  private JTextField myDherkinOptionsField;
  private EnvironmentVariablesComponent myEnvironmentVariables;
  private TextFieldWithBrowseButton myDartFile;
  private JLabel scenarioLabel;
  private CucumberDartRunnerParameters.Scope scope;

  public CucumberDartConfigurationEditorForm(@NotNull final Project project) {
    try {


      // show how to select the dart files
      DartCommandLineConfigurationEditorForm.initDartFileTextWithBrowse(project, myDartFile);

      myDartFile.addBrowseFolderListener(DartBundle.message("choose.dart.directory"), null, project,
        // Unfortunately, withFileFilter() only works for files, not directories.
        FileChooserDescriptorFactory.createSingleFolderDescriptor());
      myDartFile.addActionListener(e -> onTestDirChanged(project));

//      myScenario.setModel(
//        new DefaultComboBoxModel<>(new CucumberDartRunnerParameters.Scope[]{FOLDER, FEATURE, SCENARIO}));
//
//      myScenario.setRenderer(new ListCellRendererWrapper<CucumberDartRunnerParameters.Scope>() {
//        @Override
//        public void customize(final JList list,
//                              final CucumberDartRunnerParameters.Scope value,
//                              final int index,
//                              final boolean selected,
//                              final boolean hasFocus) {
//          setText(value.getPresentableName());
//        }
//      });

//      myScenario.addActionListener(e -> onScopeChanged());

      final DocumentAdapter dirListener = new DocumentAdapter() {
        @Override
        protected void textChanged(@NotNull final DocumentEvent e) {
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

//    myScenario.setSelectedItem(parameters.getScope());

    // what is the cucumber file we are using?
    String cukeFilePath = FileUtil.toSystemDependentName(StringUtil.notNullize(parameters.getCucumberFilePath()));
    if (parameters.getScope() == FOLDER) {
      myDirField.setText(cukeFilePath);
    }
    else {
      myFileField.setText(cukeFilePath);
    }

    myDartFile.setText(StringUtil.notNullize(parameters.getDartFilePath()));

//    myDartFileNameField.setText(
//      parameters.getScope() != FOLDER ? StringUtil.notNullize(parameters.getNameFilter()) : "");
    myDherkinOptionsField.setText(parameters.getTestRunnerOptions());
    myEnvironmentVariables.setEnvs(parameters.getEnvs());
    myEnvironmentVariables.setPassParentEnvs(parameters.isIncludeParentEnvs());

    scope = parameters.getScope();

    onScopeChanged();
  }

  @Override
  protected void applyEditorTo(@NotNull final CucumberDartRunConfiguration configuration) throws ConfigurationException {
    final CucumberDartRunnerParameters parameters = configuration.getRunnerParameters();

//    final CucumberDartRunnerParameters.Scope scope = (CucumberDartRunnerParameters.Scope) myScenario.getSelectedItem();
    parameters.setScope(scope);
    TextFieldWithBrowseButton pathSource = scope == FOLDER ? myDirField : myFileField;
    parameters.setFilePath(StringUtil.nullize(FileUtil.toSystemIndependentName(myDartFile.getText().trim())));
//    parameters.setNameFilter(StringUtil.nullize(myDartFileNameField.getText().trim()));
    parameters.setCucumberFilePath(StringUtil.nullize(FileUtil.toSystemIndependentName(pathSource.getText().trim())));
//    parameters.setTargetName(scope == FOLDER ? StringUtil.nullize(myTargetNameField.getText().trim()) : null);
    parameters.setTestRunnerOptions(StringUtil.nullize(myDherkinOptionsField.getText().trim()));
    parameters.setEnvs(myEnvironmentVariables.getEnvs());
    parameters.setIncludeParentEnvs(myEnvironmentVariables.isPassParentEnvs());
  }

  private void onScopeChanged() {
    final CucumberDartRunnerParameters.Scope scope = (CucumberDartRunnerParameters.Scope) myScenario.getSelectedItem();
//    myDherkinMainFileNameLabel.setVisible(scope == GROUP_OR_TEST_BY_NAME);
    myFileField.setVisible(scope != CucumberDartRunnerParameters.Scope.FOLDER);

    boolean folderMode = scope == FOLDER;
    boolean projectWithoutPubspec = Registry.is("dart.projects.without.pubspec", false);
    myFileField.setVisible(!folderMode);
    myTestFileLabel.setVisible(!folderMode);
    myDirField.setVisible(folderMode);
    myDirLabel.setVisible(folderMode);
    myScenario.setVisible(scope == SCENARIO);
    scenarioLabel.setVisible(scope == SCENARIO);
  }

  private void onTestDirChanged(Project project) {
    if (!isDirApplicable(myDirField.getText(), project)) {
      myDirField.getTextField().setForeground(JBColor.RED);
      final String message = DartBundle.message("test.dir.not.in.project");
      myDirField.getTextField().setToolTipText(message);
    }
    else {
      myDirField.getTextField().setForeground(UIUtil.getFieldForegroundColor());
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
