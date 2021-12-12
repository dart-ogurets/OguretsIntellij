package dev.bluebiscuitdesign.cucumber.dart.steps.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.CommandLineState;
import com.intellij.execution.configurations.CommandLineTokenizer;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.TextConsoleBuilder;
import com.intellij.execution.filters.TextConsoleBuilderImpl;
import com.intellij.execution.filters.UrlFilter;
import com.intellij.execution.process.ColoredProcessHandler;
import com.intellij.execution.process.OSProcessHandler;
import com.intellij.execution.process.ProcessAdapter;
import com.intellij.execution.process.ProcessEvent;
import com.intellij.execution.process.ProcessHandler;
import com.intellij.execution.process.ProcessTerminatedListener;
import com.intellij.execution.runners.ExecutionEnvironment;
import com.intellij.execution.runners.ProgramRunner;
import com.intellij.execution.testframework.TestConsoleProperties;
import com.intellij.execution.testframework.actions.AbstractRerunFailedTestsAction;
import com.intellij.execution.testframework.autotest.ToggleAutoTestAction;
import com.intellij.execution.testframework.sm.SMCustomMessagesParsing;
import com.intellij.execution.testframework.sm.SMTestRunnerConnectionUtil;
import com.intellij.execution.testframework.sm.runner.OutputToGeneralTestEventsConverter;
import com.intellij.execution.testframework.sm.runner.SMTRunnerConsoleProperties;
import com.intellij.execution.testframework.sm.runner.SMTestLocator;
import com.intellij.execution.ui.ConsoleView;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.Separator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.Key;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.CharsetToolkit;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiFile;
import com.intellij.util.Consumer;
import com.intellij.util.net.NetUtils;
import com.jetbrains.lang.dart.DartBundle;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.coverage.DartCoverageProgramRunner;
import com.jetbrains.lang.dart.ide.errorTreeView.DartProblemsView;
import com.jetbrains.lang.dart.ide.runner.DartConsoleFilter;
import com.jetbrains.lang.dart.ide.runner.DartExecutionHelper;
import com.jetbrains.lang.dart.ide.runner.DartRelativePathsConsoleFilter;
import com.jetbrains.lang.dart.ide.runner.base.DartRunConfiguration;
import com.jetbrains.lang.dart.ide.runner.server.OpenDartObservatoryUrlAction;
import com.jetbrains.lang.dart.ide.runner.util.DartTestLocationProvider;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.sdk.DartSdkUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

// we have to copy a bunch of stuff form DartCommandLineRunningState because it is
// written to force us to use an existing Dart File, which we don't want, we want to be able to generate one.

public class CucumberDartRunningTestState extends CommandLineState {
	public static final String DART_FRAMEWORK_NAME = "cucumber";
	public static final String DART_VM_OPTIONS_ENV_VAR = "DART_VM_OPTIONS";
  private final Collection<Filter> myConsoleFilters = new ArrayList<>();
  public int myObservatoryPort;
  protected final @NotNull
  CucumberDartRunnerParameters myRunnerParameters;
  private final Collection<Consumer<String>> myObservatoryUrlConsumers = new ArrayList<>();

  public CucumberDartRunningTestState(@NotNull ExecutionEnvironment env) throws ExecutionException {
		super(env);

    myRunnerParameters = ((CucumberDartRunConfiguration)env.getRunProfile()).getRunnerParameters().clone();

    final Project project = env.getProject();
    try {
      VirtualFile dartFile = dartRunPath((CucumberDartRunConfiguration)env.getRunProfile());
      myRunnerParameters.setFilePath(dartFile.getPath());
      myRunnerParameters.check(project);
    }
    catch (RuntimeConfigurationError e) {
      throw new ExecutionException(e);
    }

    final TextConsoleBuilder builder = getConsoleBuilder();
    if (builder instanceof TextConsoleBuilderImpl) {
      ((TextConsoleBuilderImpl)builder).setUsePredefinedMessageFilter(false);
    }

    try {
      builder.addFilter(new DartConsoleFilter(project, myRunnerParameters.getDartFileOrDirectory()));
      builder.addFilter(new DartRelativePathsConsoleFilter(project, myRunnerParameters.computeProcessWorkingDirectory(project)));
      builder.addFilter(new UrlFilter());
    }
    catch (RuntimeConfigurationError e) { /* can't happen because already checked */}
	}

  protected VirtualFile dartRunPath(CucumberDartRunConfiguration runConfig) throws RuntimeConfigurationError {
    VirtualFile dartFile = null;
    String myFilePath = runConfig.getRunnerParameters().getDartFilePath();

    if (myFilePath == null) {
      assert runConfig.getRunnerParameters().getCucumberFilePath() != null;
      PsiFile generatedRunnableFile = null;
      try {
        generatedRunnableFile = CucumberDartRunConfigurationProducer.generateRunnableFile(runConfig.getProject(),
          LocalFileSystem.getInstance().findFileByPath(runConfig.getRunnerParameters().getCucumberFilePath()));
      } catch (IOException e) {
        throw new RuntimeConfigurationError(DartBundle.message("not.a.dart.file.or.directory", "generated-file"));
      }

      if (generatedRunnableFile != null) {
        dartFile = generatedRunnableFile.getVirtualFile();
      }
    } else {
      return LocalFileSystem.getInstance().findFileByPath(myFilePath);
    }

    myFilePath = "unknown.dart";

    if (dartFile == null) {
      throw new RuntimeConfigurationError(DartBundle.message("dart.file.not.found", FileUtil.toSystemDependentName(myFilePath)));
    } else if (dartFile.getFileType() != DartFileType.INSTANCE) {
      throw new RuntimeConfigurationError(DartBundle.message("not.a.dart.file.or.directory", FileUtil.toSystemDependentName(myFilePath)));
    }

    return dartFile;
  }


  @NotNull
  @Override
  protected AnAction[] createActions(final ConsoleView console, final ProcessHandler processHandler, final Executor executor) {
    // These actions are effectively added only to the Run tool window. For Debug see DartCommandLineDebugProcess.registerAdditionalActions()
    final List<AnAction> actions = new ArrayList<>(Arrays.asList(super.createActions(console, processHandler, executor)));
    addObservatoryActions(actions, processHandler);
    return actions.toArray(AnAction.EMPTY_ARRAY);
  }

  protected void addObservatoryActions(List<AnAction> actions, final ProcessHandler processHandler) {
    actions.add(new Separator());

    final OpenDartObservatoryUrlAction openObservatoryAction =
      new OpenDartObservatoryUrlAction(null, () -> !processHandler.isProcessTerminated());
    addObservatoryUrlConsumer(openObservatoryAction::setUrl);

    actions.add(openObservatoryAction);
  }

  public void addObservatoryUrlConsumer(@NotNull final Consumer<String> consumer) {
    myObservatoryUrlConsumers.add(consumer);
  }

	@Override
	@NotNull
	public ExecutionResult execute(final @NotNull Executor executor, final @NotNull ProgramRunner runner) throws ExecutionException {
		final ProcessHandler processHandler = startProcess();
		final ConsoleView consoleView = createConsole(getEnvironment(), processHandler, myConsoleFilters);
		consoleView.attachToProcess(processHandler);

		final DefaultExecutionResult executionResult =
			new DefaultExecutionResult(consoleView, processHandler, createActions(consoleView, processHandler, executor));

			executionResult.setRestartActions(new ToggleAutoTestAction());

		return executionResult;
	}

  @Override
  public void addConsoleFilters(Filter... filters) {
    myConsoleFilters.addAll(Arrays.asList(filters));
  }

  private static ConsoleView createConsole(@NotNull ExecutionEnvironment env, ProcessHandler processHandler, Collection<Filter> myConsoleFilters)
    throws ExecutionException {
		final Project project = env.getProject();
		final DartRunConfiguration runConfiguration = (DartRunConfiguration) env.getRunProfile();
		final CucumberDartRunnerParameters runnerParameters = (CucumberDartRunnerParameters) runConfiguration.getRunnerParameters();

		final TestConsoleProperties testConsoleProperties = new DartConsoleProperties(runConfiguration, env);
		final ConsoleView consoleView = SMTestRunnerConnectionUtil.createConsole(DART_FRAMEWORK_NAME, testConsoleProperties);

		try {
			final VirtualFile dartFile = runnerParameters.getDartFileOrDirectory();
			consoleView.addMessageFilter(new DartConsoleFilter(project, dartFile));
      myConsoleFilters.forEach((filter) -> consoleView.addMessageFilter(filter));
//			consoleView.addMessageFilter(new DartRelativePathsConsoleFilter(project, runnerParameters.computeProcessWorkingDirectory(project)));
//			consoleView.addMessageFilter(new UrlFilter());
		} catch (RuntimeConfigurationError ignore) {/* can't happen because already checked */}

		Disposer.register(project, consoleView);
		return consoleView;
	}

	// using: https://medium.com/flutter-community/hot-reload-for-flutter-integration-tests-e0478b63bd54
	@NotNull
	@Override
	protected ProcessHandler startProcess() throws ExecutionException {

		Project project = getEnvironment().getProject();

		DartSdk sdk = DartSdk.getDartSdk(project);
		if (sdk == null) throw new ExecutionException("Dart SDK cannot be found"); // can't happen, already checked

		CucumberDartRunnerParameters params = getParameters();

    List<String> commands = new ArrayList<>();

    if (CucumberDartRunnerParameters.isFlutterDriverExecutable(params)) {
      commands.add("driver");
      params.setArguments(String.join(" ", commands));

    } else if (DefaultDebugExecutor.EXECUTOR_ID.equals(getEnvironment().getExecutor().getId())) {
      // cannot support debugging with flutter driver, no idea which one they want to debug!!
      commands.add("--pause_isolates_on_start");
      try {
        this.myObservatoryPort = NetUtils.findAvailableSocketPort();
      }
      catch (IOException e) {
        throw new ExecutionException(e);
      }

      commands.add("--enable-vm-service:" + myObservatoryPort);
      params.setVMOptions(String.join(" ", commands));
      params.setCheckedModeOrEnableAsserts(true);
    }
    else {
      params.setCheckedModeOrEnableAsserts(true);
    }

		// working directory is not configurable in UI because there's only one valid value that we calculate ourselves
		params.setWorkingDirectory(params.computeProcessWorkingDirectory(project));

    final GeneralCommandLine commandLine = createCommandLine();

    // Workaround for "Observatory listening on ..." message that is concatenated (without line break) with the message following it
    final OSProcessHandler processHandler = new ColoredProcessHandler(commandLine) {
      @Override
      public void coloredTextAvailable(@NotNull String text, @NotNull Key attributes) {
        if (text.startsWith(DartConsoleFilter.OBSERVATORY_LISTENING_ON)) {
          text += "\n";
        }
        super.coloredTextAvailable(text, attributes);
      }
    };

    processHandler.addProcessListener(new ProcessAdapter() {
      @Override
      public void onTextAvailable(@NotNull final ProcessEvent event, @NotNull final Key outputType) {
        final String prefix = DartConsoleFilter.OBSERVATORY_LISTENING_ON + "http://";
        final String text = event.getText().trim();
        if (text.startsWith(prefix)) {
          processHandler.removeProcessListener(this);
          final String url = "http://" + text.substring(prefix.length());
          for (Consumer<String> consumer: myObservatoryUrlConsumers) {
            consumer.consume(url);
          }
        }
      }
    });

    // Check for and display any analysis errors when we launch a Dart app.
    try {
      final DartRunConfiguration dartRunConfiguration = (DartRunConfiguration)getEnvironment().getRunProfile();
      final VirtualFile launchFile = dartRunConfiguration.getRunnerParameters().getDartFileOrDirectory();
      final String message = ("<a href='" + DartProblemsView.OPEN_DART_ANALYSIS_LINK + "'>Analysis issues</a> may affect " +
        "the execution of '" + dartRunConfiguration.getName() + "'.");
      DartExecutionHelper.displayIssues(project, launchFile, message, dartRunConfiguration.getIcon());
    }
    catch (RuntimeConfigurationError error) {
      DartExecutionHelper.clearIssueNotifications(project);
    }

    ProcessTerminatedListener.attach(processHandler, getEnvironment().getProject());
    return processHandler;
	}

  private GeneralCommandLine createCommandLine() throws ExecutionException {
    final DartSdk sdk = DartSdk.getDartSdk(getEnvironment().getProject());
    if (sdk == null) {
      throw new ExecutionException(DartBundle.message("dart.sdk.is.not.configured"));
    }

    final GeneralCommandLine commandLine = new GeneralCommandLine()
      .withWorkDirectory(myRunnerParameters.computeProcessWorkingDirectory(getEnvironment().getProject()));
    commandLine.setCharset(StandardCharsets.UTF_8);
    commandLine.setExePath(FileUtil.toSystemDependentName(getExePath(sdk)));
    commandLine.getEnvironment().putAll(myRunnerParameters.getEnvs());
    commandLine
      .withParentEnvironmentType(myRunnerParameters.isIncludeParentEnvs() ? GeneralCommandLine.ParentEnvironmentType.CONSOLE : GeneralCommandLine.ParentEnvironmentType.NONE);
    setupParameters(sdk, commandLine);

    return commandLine;
  }

  private void setupParameters(@NotNull final DartSdk sdk,
                               @NotNull final GeneralCommandLine commandLine) throws ExecutionException {
    int customObservatoryPort = -1;

    final String vmOptions = myRunnerParameters.getVMOptions();
    if (vmOptions != null) {
      final StringTokenizer vmOptionsTokenizer = new CommandLineTokenizer(vmOptions);
      while (vmOptionsTokenizer.hasMoreTokens()) {
        final String vmOption = vmOptionsTokenizer.nextToken();
        addVmOption(commandLine, vmOption);

        try {
          if (vmOption.equals("--enable-vm-service") || vmOption.equals("--observe")) {
            customObservatoryPort = 8181; // default port, see https://www.dartlang.org/tools/dart-vm/
          }
          else if (vmOption.startsWith("--enable-vm-service:")) {
            customObservatoryPort = parseIntBeforeSlash(vmOption.substring("--enable-vm-service:".length()));
          }
          else if (vmOption.startsWith("--observe:")) {
            customObservatoryPort = parseIntBeforeSlash(vmOption.substring("--observe:".length()));
          }
        }
        catch (NumberFormatException ignore) {/**/}
      }
    }

    if (DefaultDebugExecutor.EXECUTOR_ID.equals(getEnvironment().getExecutor().getId())) {
      addVmOption(commandLine, "--pause_isolates_on_start");
    }

    if (customObservatoryPort > 0) {
      myObservatoryPort = customObservatoryPort;
    }
    else {
      try {
        myObservatoryPort = NetUtils.findAvailableSocketPort();
      }
      catch (IOException e) {
        throw new ExecutionException(e);
      }

      addVmOption(commandLine, "--enable-vm-service:" + myObservatoryPort);

      if (getEnvironment().getRunner() instanceof DartCoverageProgramRunner) {
        addVmOption(commandLine, "--pause-isolates-on-exit");
      }
    }

    appendParamsAfterVmOptionsBeforeArgs(commandLine);

    final String arguments = myRunnerParameters.getArguments();
    if (arguments != null) {
      StringTokenizer argumentsTokenizer = new CommandLineTokenizer(arguments);
      while (argumentsTokenizer.hasMoreTokens()) {
        commandLine.addParameter(argumentsTokenizer.nextToken());
      }
    }
  }

  private static int parseIntBeforeSlash(@NotNull final String s) throws NumberFormatException {
    // "5858" or "5858/0.0.0.0"
    final int index = s.indexOf('/');
    return Integer.parseInt(index > 0 ? s.substring(0, index) : s);
  }

  protected void appendParamsAfterVmOptionsBeforeArgs(@NotNull final GeneralCommandLine commandLine) throws ExecutionException {
    final VirtualFile dartFile;
    try {
      dartFile = myRunnerParameters.getDartFileOrDirectory();
    }
    catch (RuntimeConfigurationError e) {
      throw new ExecutionException(e);
    }

    // this one just doesn't work via the environment options and we only want it on our app, we don't
    // want to pass it through to any app we run
    if (myRunnerParameters.isCheckedModeOrEnableAsserts()) {
      commandLine.addParameter("--enable-asserts");
    }
    vmOptions.forEach(commandLine::addParameter);

    commandLine.addParameter(FileUtil.toSystemDependentName(dartFile.getPath()));
  }

	@NotNull
	protected String getExePath(@NotNull final DartSdk sdk) {
		final CucumberDartRunnerParameters runnerParameters = getParameters();
		if (runnerParameters.isFlutterEnabled()) {
		  if (CucumberDartRunnerParameters.isFlutterDriverExecutable(runnerParameters)) {
			  return FlutterSdk.getFlutterSdk(getEnvironment().getProject()).getExePath();
      }
		}

    return DartSdkUtil.getDartExePath(sdk);
	}

	protected List<String> vmOptions = new ArrayList<>();

	protected void addVmOption(@NotNull final GeneralCommandLine commandLine, @NotNull final String option) {
    this.vmOptions.add(option);
	}

	CucumberDartRunnerParameters getParameters() {
		return (CucumberDartRunnerParameters) myRunnerParameters;
	}

	private static class DartConsoleProperties extends SMTRunnerConsoleProperties implements SMCustomMessagesParsing {
		DartConsoleProperties(DartRunConfiguration runConfiguration, ExecutionEnvironment env) {
			super(runConfiguration, DART_FRAMEWORK_NAME, env.getExecutor());
			setUsePredefinedMessageFilter(false);
			setIdBasedTestTree(false);
		}

		@Nullable
		@Override
		public SMTestLocator getTestLocator() {
			return DartTestLocationProvider.INSTANCE;
		}

		@Override
		public OutputToGeneralTestEventsConverter createTestEventsConverter(@NotNull String testFrameworkName,
		                                                                    @NotNull TestConsoleProperties consoleProperties) {
//			final DartRunConfiguration runConfiguration = (DartRunConfiguration) getConfiguration();
//			try {
//				final VirtualFile file = runConfiguration.getRunnerParameters().getDartFileOrDirectory();
				return new OutputToGeneralTestEventsConverter(testFrameworkName, consoleProperties);
//			} catch (RuntimeConfigurationError error) {
//				throw new RuntimeException(error); // can't happen, already checked
//			}
		}

		@Nullable
		@Override
		public AbstractRerunFailedTestsAction createRerunFailedTestsAction(ConsoleView consoleView) {
			return null;
//			if (ActionManager.getInstance().getAction("RerunFailedTests") == null) return null; // backward compatibility
//
//			DartTestRerunnerAction action = new DartTestRerunnerAction(consoleView);
//			action.init(this);
//			return action;
		}
	}
}
