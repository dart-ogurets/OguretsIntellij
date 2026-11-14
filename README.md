# Ogurets for Jetbrains IDE's
<!-- Plugin description -->
This plugin brings full support for [Ogurets](https://github.com/dart-ogurets/Ogurets) (Cucumber for Dart) to IntelliJ IDEA,
piggybacking on JetBrains' existing Cucumber plugins: Gherkin feature files with step definitions written in Dart.
It should work in any variant that has the Gherkin and Dart plugins installed (including Android Studio),
and if it detects Flutter, it will also support Flutter FlutterDriver tests.

The following coding assistance features are available:

- Code completion
- Search for usages
- Run configurations
- Debugging

The source is available on GitHub at [OguretsIntellij](https://github.com/dart-ogurets/OguretsIntellij).
<!-- Plugin description end -->


## Features

- Full support for creating and navigating to cucumber steps inside dart classes
- Creation of dart classes that hold the steps
- Full run and _debug_ support for Features, Feature folders and Scenarios
- Full stack trace following for failures
- Automatic run configuration generation for your IDEA Ogurets Run Configurations
- Recognition of Given/When/Then annotations as regexes as such so they are treated properly by IDEA
- Full integration with the Test panel, with Jump to Source for IntelliJ

## Authors

- _Richard Vowles_, Software Developer (https://www.linkedin.com/in/richard-vowles-72035193/)
- _Irina Southwell_, Principal Engineer (https://www.linkedin.com/in/irina-southwell-9727a422/)
