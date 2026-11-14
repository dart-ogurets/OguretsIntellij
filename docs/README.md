# Setting up development environment
It's not that easy to start development on an IntelliJ IDEA plugin, but we're here to help!

## Prerequisites
We presume you are using IntelliJ IDEA as your IDE for developing this plugin.
You'll first need to do some general setup in order for your IDE.
The steps required to do so can be found at https://plugins.jetbrains.com/docs/intellij/developing-plugins.html

To get the hang of it, we recommend you first go and try to set up a simple sample plugin from scratch before continuing.

## JDK
The plugin is built for IDEA `253+` (2025.3 and later), which requires `Java 21`.
The Gradle build uses a Java toolchain, so a JDK 21 is downloaded automatically (via the foojay resolver) if you don't have one installed.
Gradle itself can run on any JDK from 17 upwards.

After cloning, open the `Gradle tool window` and click `Reload All Gradle Projects`; the IntelliJ Platform, Dart and Gherkin
plugins are resolved by the [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html).

## Build and test using Gradle
This seems to be the preferred way for IntelliJ plugin development, so we just go with the flow.
We started from the [GitHub template](https://plugins.jetbrains.com/docs/intellij/plugin-github-template.html) 
([repository](https://github.com/JetBrains/intellij-platform-plugin-template)), 
which provides a bunch of setup that will enable us to focus on plugin development.

### Build gradle project:
1. Open the `Gradle tool window` and click on `Reload All Gradle Projects`
2. In your IDE, click `Build Project (Ctrl + F9)`

When you run the `Run Plugin` Gradle run configuration, a new IDEA will start, with the current plugin (and dependency plugins) enabled.

## IDEA versions
The minimum supported IDEA build is set in `gradle.properties` using `pluginSinceBuild`. No `until-build` is set, so the plugin
stays installable on newer IDEs; the `verifyPlugin` task (also run in CI) checks binary compatibility against the recommended IDE releases.

The version of IDEA the plugin is compiled against, and run with during development, is also configured in `gradle.properties`.
Build against the lowest supported version so you don't accidentally use newer APIs:
```.properties
pluginSinceBuild = 253
platformVersion = 2025.3.6.2
```
More info can be found at [IntelliJ Platform Gradle Plugin](https://plugins.jetbrains.com/docs/intellij/tools-intellij-platform-gradle-plugin.html).

Some notes:
 - https://www.jetbrains.com/idea/download/other.html contains an overview of all IDEA versions
 - from 2025.3 onwards there is a single unified `IntelliJ IDEA` distribution (no separate Community/Ultimate), referenced in `build.gradle.kts` via `intellijIdea(...)`
 - the build numbers used in `pluginSinceBuild` are a sort of "short-hand" for the actual release numbers, eg:
   - eg: `223.*` == `2022.3.*`
   - eg: `253.*` == `2025.3.*`
   - See [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html#build-number-format) for more on this
 - depending on the IDEA you are targeting, you'll also need to compile using the correct Java version (see the release notes in
   [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html#intellij-platform-based-products-of-recent-ide-versions)),
   adjust the `java.toolchain` block in `build.gradle.kts` accordingly

## Plugin dependencies
This plugin depends on 2 other plugins:
 - [Gherkin](https://plugins.jetbrains.com/plugin/9164-gherkin/versions)
 - [Dart](https://plugins.jetbrains.com/plugin/6351-dart/versions)

This is configured in `gradle.properties`; pick versions compatible with `platformVersion`:
```.properties
platformPlugins = gherkin:253.28294.218, Dart:507.0.0
```

These are added to the compile classpath and installed into the sandbox IDE by the IntelliJ Platform Gradle Plugin.
Do not add their jars as regular `implementation` dependencies, otherwise you'll get `LinkageError`s because the classes are loaded
by two different classloaders (`PluginClassLoader` and `PathClassLoader`).

## Releasing plugin
 - environment variables:
   - `PUBLISH_TOKEN`

