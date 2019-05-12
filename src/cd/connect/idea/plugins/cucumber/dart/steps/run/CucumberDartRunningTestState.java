package cd.connect.idea.plugins.cucumber.dart.steps.run;

import com.intellij.execution.DefaultExecutionResult;
import com.intellij.execution.ExecutionException;
import com.intellij.execution.ExecutionResult;
import com.intellij.execution.Executor;
import com.intellij.execution.configurations.GeneralCommandLine;
import com.intellij.execution.configurations.RuntimeConfigurationError;
import com.intellij.execution.executors.DefaultDebugExecutor;
import com.intellij.execution.executors.DefaultRunExecutor;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.process.ProcessHandler;
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
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.net.NetUtils;
import com.jetbrains.lang.dart.coverage.DartCoverageProgramRunner;
import com.jetbrains.lang.dart.ide.runner.DartConsoleFilter;
import com.jetbrains.lang.dart.ide.runner.base.DartRunConfiguration;
import com.jetbrains.lang.dart.ide.runner.server.DartCommandLineRunningState;
import com.jetbrains.lang.dart.ide.runner.test.DartTestEventsConverter;
import com.jetbrains.lang.dart.ide.runner.util.DartTestLocationProvider;
import com.jetbrains.lang.dart.sdk.DartSdk;
import com.jetbrains.lang.dart.sdk.DartSdkUtil;
import com.jetbrains.lang.dart.util.DartUrlResolver;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

public class CucumberDartRunningTestState extends DartCommandLineRunningState {
	public static final String DART_FRAMEWORK_NAME = "cucumber";
	private static final String RUN_COMMAND = "run";
	private static final String TEST_PACKAGE_SPEC = "test";
	private static final String EXPANDED_REPORTER_OPTION = "-r json";
	public static final String DART_VM_OPTIONS_ENV_VAR = "DART_VM_OPTIONS";
  private final Collection<Filter> myConsoleFilters = new ArrayList<>();
  private int myObservatoryPort;

	public CucumberDartRunningTestState(@NotNull ExecutionEnvironment env) throws ExecutionException {
		super(env);
	}

	@Override
	@NotNull
	public ExecutionResult execute(final @NotNull Executor executor, final @NotNull ProgramRunner runner) throws ExecutionException {
		final ProcessHandler processHandler = startProcess();
		final ConsoleView consoleView = createConsole(getEnvironment(), processHandler, myConsoleFilters);
		consoleView.attachToProcess(processHandler);

		final DefaultExecutionResult executionResult =
			new DefaultExecutionResult(consoleView, processHandler, createActions(consoleView, processHandler, executor));

//		if (ActionManager.getInstance().getAction("RerunFailedTests") != null) {
//			DartConsoleProperties properties = (DartConsoleProperties) ((SMTRunnerConsoleView) consoleView).getProperties();
//			AbstractRerunFailedTestsAction rerunFailedTestsAction = properties.createRerunFailedTestsAction(consoleView);
//			assert rerunFailedTestsAction != null;
//			rerunFailedTestsAction.setModelProvider(((SMTRunnerConsoleView) consoleView)::getResultsViewer);
//			executionResult.setRestartActions(rerunFailedTestsAction, new ToggleAutoTestAction());
//		} else {
			executionResult.setRestartActions(new ToggleAutoTestAction());
//		}

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
    }


//      if (getEnvironment().getRunner() instanceof DartCoverageProgramRunner) {
//        addVmOption(commandLine, "--pause-isolates-on-exit");
//      }

    
		final String filePath = params.getFilePath();
		if (filePath != null && filePath.contains(" ")) {
		  commands.add("\"" + filePath + "\"");
		} else {
			commands.add(filePath);
		}


		params.setArguments(String.join(" ", commands));
		params.setCheckedModeOrEnableAsserts(false);
		// working directory is not configurable in UI because there's only one valid value that we calculate ourselves
		params.setWorkingDirectory(params.computeProcessWorkingDirectory(project));

		return super.startProcess();
	}

	@NotNull
	@Override
	protected String getExePath(@NotNull final DartSdk sdk) {
		final CucumberDartRunnerParameters runnerParameters = getParameters();
		if (runnerParameters.isFlutterEnabled()) {
		  if (CucumberDartRunnerParameters.isFlutterDriverExecutable(runnerParameters)) {
			  return FlutterSdk.getFlutterSdk(getEnvironment().getProject()).getExePath();
      }
		}

    return DartSdkUtil.getDartExePath(sdk);

//		return DartSdkUtil.getPubPath(sdk);
	}

	@Override
	protected void appendParamsAfterVmOptionsBeforeArgs(@NotNull GeneralCommandLine commandLine) {
		// nothing needed
	}

	@Override
	protected void addVmOption(@NotNull final GeneralCommandLine commandLine, @NotNull final String option) {
		final String arguments = StringUtil.notNullize(myRunnerParameters.getArguments());
		if (DefaultRunExecutor.EXECUTOR_ID.equals(getEnvironment().getExecutor().getId()) &&
			option.startsWith("--enable-vm-service:") &&
			(arguments.startsWith("-p ") || arguments.contains(" -p "))) {
			// When we start browser-targeted tests then there are 2 dart processes spawned: parent (pub) and child (tests).
			// If we add --enable-vm-service option to the DART_VM_OPTIONS env var then it will apply for both processes and will obviously
			// fail for the child process (because the port will be already occupied by the parent one).
			// Setting --enable-vm-service option for the parent process doesn't make much sense, so we skip it.
			return;
		}

		String options = commandLine.getEnvironment().get(DART_VM_OPTIONS_ENV_VAR);
		if (StringUtil.isEmpty(options)) {
			commandLine.getEnvironment().put(DART_VM_OPTIONS_ENV_VAR, "");
		} else {
			commandLine.getEnvironment().put(DART_VM_OPTIONS_ENV_VAR, options + " " + option);
		}
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
