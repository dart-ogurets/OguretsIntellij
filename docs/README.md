# Setting up development environment
It's not that easy to start development on an IntelliJ IDEA plugin, but we're here to help!

## Prerequisites
We presume you are using IntelliJ IDEA as your IDE for developing this plugin.
You'll first need to do some general setup in order for your IDE.
The steps required to do so can be found at https://plugins.jetbrains.com/docs/intellij/developing-plugins.html

To get the hang of it, we recommend you first go and try to set up a simple sample plugin from scratch before continuing.

## IDEA Project Structure settings
Since our plugin is built for IDEA version `223+`, `Java 17` is required (more info further down this doc),
we need to make sure our development IDEA is configured to use `JDK 17` as well:
 - `File` > `Project Structure` 
   - `Project Settings`:
     - `Modules`:
        - Set `Module SDK` to version `17`
     - `Libraries`:
       - Make sure the following libraries are present:
         - `Gradle: io.cucumber:gherkin:*.*.*`
         - `Gradle: io.cucumber:messages:*.*.*`
         - `Gradle: org.jetbrains:annotations:*.*.*`
         - `Gradle: unzipped.com.jetbrains.plugins:Dart:unzipped.com.jetbrains.plugins:*.*.*`
         - `Gradle: unzipped.com.jetbrains.plugins:gherkin:unzipped.com.jetbrains.plugins:*.*.*`
       - If the latter 2 aren't present, reload the Gradle project in your IDE
   - `Platform settings`
     - `SDKs`
        - Set `IntelliJ IDEA IU-22.*.*` > `Internal Java Platform` to version `17`


## Build and test using Gradle and Kotlin
This seems to be the preferred way for IntelliJ plugin development, so we just go with the flow.
We started from the [GitHub template](https://plugins.jetbrains.com/docs/intellij/plugin-github-template.html) 
([repository](https://github.com/JetBrains/intellij-platform-plugin-template)), 
which provides a bunch of setup that will enable us to focus on plugin development.

### Build gradle project:
1. Open the `Gradle tool window` and click on `Reload All Gradle Projects`
2. In your IDE, click `Build Project (Ctrl + F9)`

When you run the `Run Plugin` Gradle run configuration, a new IDEA will start, with the current plugin (and dependency plugins) enabled.

## IDEA versions
Plugins need to define their `since` and `until` IDEA versions.
This is done in the `gradle.properties` file using `pluginSinceBuild` and `pluginUntilBuild` properties.

The version of IDEA in which you want to run the plugin during development, can also be configured in `gradle.properties`:
```.properties
platformType = IC
platformVersion = 2022.3
```
More info can be found at [Gradle IntelliJ Plugin Configuration](https://plugins.jetbrains.com/docs/intellij/tools-gradle-intellij-plugin.html#configuration-intellij-extension).

Some notes:
 - https://www.jetbrains.com/idea/download/other.html contains an overview of all IDEA versions
 - the build numbers used in `pluginSinceBuild` and `pluginUntilBuild` are a sort of "short-hand" for the actual release numbers, eg:
   - eg: `211.*` == `2021.1.*`
   - eg: `223.*` == `2022.3.*`
   - See [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html#build-number-format) for more on this
 - depending on the IDEA you are targetting, you'll also need to make sure you compile using the correct Java version.
   - the release notes in [Build Number Ranges](https://plugins.jetbrains.com/docs/intellij/build-number-ranges.html#intellij-platform-based-products-of-recent-ide-versions) will tell you which Java version is required
   - in `build.gradle.kts` make sure to adjust `jvmToolchain` to reflect the correct Java version

## Plugin dependencies
This plugin depends on 2 other plugins:
 - [Gherkin](https://plugins.jetbrains.com/plugin/9164-gherkin/versions)
 - [Dart](https://plugins.jetbrains.com/plugin/6351-dart/versions)

This is configured in `gradle.properties`
```.properties
platformPlugins = gherkin:223.7571.113, dart:223.7571.203
```

Please note that the 2 dependencies from `platformPlugins` will also result in the defined classes to be included as dependencies in our project.
If you don't add them there, you would have to add them manually in the `build.gradle.kts` file, like this:
```.kts
  ...
  dependencies {
    ...
    // https://plugins.jetbrains.com/plugin/6351-dart/versions
    api(files("lib/Dart-223.7571.203.jar"))
    // https://plugins.jetbrains.com/plugin/9164-gherkin/versions
    api(files("lib/gherkin-223.7571.113.jar"))
    ...
  }
  ...
```
When set up correctly, these plugins are also downloaded and enabled when running the `Run Plugin` Gradle run configuration.

However, when you do define them in your `build.gradle.kts` depdencies, then you'll get `LinkageError`s because they are loaded by two different classloaders:
 - `PluginClassLoader` (because of plugin dependency)
 - `PathClassLoader` (because `build.gradle.kts` dependency)

Important to note here is that the versions of the plugins should be compatible, but you'll notice that soon enough when running the plugin.



## Releasing plugin
 - environment variables:
   - `PUBLISH_TOKEN`

