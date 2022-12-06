package dev.bluebiscuitdesign.cucumber.dart.steps.run;

// bits taken from https://github.com/flutter/flutter-intellij/blob/master/src/io/flutter/sdk/*.java
// its an amalgam just to allow us not to have to include Flutter as a dependency if no-one is ever using it

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.SystemInfo;
import com.intellij.openapi.util.io.FileUtil;
import com.intellij.openapi.vfs.LocalFileSystem;
import com.intellij.openapi.vfs.VirtualFile;
import com.jetbrains.lang.dart.sdk.DartSdk;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class FlutterSdk {
	public static final String FLUTTER_SDK_GLOBAL_LIB_NAME = "Flutter SDK";

	public static final String DART_SDK_SUFFIX = "/bin/cache/dart-sdk";

	private static final String DART_CORE_SUFFIX = DART_SDK_SUFFIX + "/lib/core";

	private static final Logger LOG = Logger.getInstance(FlutterSdk.class);

	private static final Map<String, FlutterSdk> projectSdkCache = new HashMap<>();

	private final @NotNull VirtualFile myHome;
	private final Map<String, String> cachedConfigValues = new HashMap<>();

	private FlutterSdk(@NotNull final VirtualFile home) {
		myHome = home;
	}

	public String getExePath() {
		return FileUtil.toSystemDependentName(myHome.getPath() + "/bin/" + flutterScriptName());
	}

	@NotNull
	public static String flutterScriptName() {
		return SystemInfo.isWindows ? "flutter.bat" : "flutter";
	}

	/**
	 * Return the FlutterSdk for the given project.
	 * <p>
	 * Returns null if the Dart SDK is not set or does not exist.
	 */
	@Nullable
	public static FlutterSdk getFlutterSdk(@NotNull final Project project) {
		if (project.isDisposed()) {
			return null;
		}
		final DartSdk dartSdk = DartSdk.getDartSdk(project);
		if (dartSdk == null) {
			return null;
		}

		final String dartPath = dartSdk.getHomePath();
		if (!dartPath.endsWith(DART_SDK_SUFFIX)) {
			return null;
		}

		final String sdkPath = dartPath.substring(0, dartPath.length() - DART_SDK_SUFFIX.length());

		// Cache based on the project and path ('e41cfa3d:/Users/devoncarew/projects/flutter/flutter').
		final String cacheKey = project.getLocationHash() + ":" + sdkPath;
		return projectSdkCache.computeIfAbsent(cacheKey, s -> forPath(sdkPath));
	}

	@Nullable
	public static FlutterSdk forPath(@NotNull final String path) {
		final VirtualFile home = LocalFileSystem.getInstance().findFileByPath(path);
		if (home == null || !isFlutterSdkHome(path)) {
			return null;
		}
		else {
			return new FlutterSdk(home);
		}
	}

	public static boolean isFlutterSdkHome(@NotNull final String path) {
		final File flutterPubspecFile = new File(path + "/packages/flutter/pubspec.yaml");
		final File flutterToolFile = new File(path + "/bin/flutter");
		final File dartLibFolder = new File(path + "/bin/cache/dart-sdk/lib");
		return flutterPubspecFile.isFile() && flutterToolFile.isFile() && dartLibFolder.isDirectory();
	}
}
