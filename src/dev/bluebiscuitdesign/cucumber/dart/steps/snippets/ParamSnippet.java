package dev.bluebiscuitdesign.cucumber.dart.steps.snippets;

import cucumber.runtime.snippets.Snippet;

import java.util.List;

public interface ParamSnippet extends Snippet {

  String paramArguments(List<ArgumentParam> argumentTypes);

  class ArgumentParam {
    public Class<?> clazz;
    public String name;

    private ArgumentParam(Builder builder) {
      clazz = builder.clazz;
      name = builder.name;
    }

    public static final class Builder {
      private Class<?> clazz;
      private String name;

      public Builder() {
      }

      public Builder clazz(Class<?> val) {
        clazz = val;
        return this;
      }

      public Builder name(String val) {
        name = val;
        return this;
      }

      public ArgumentParam build() {
        return new ArgumentParam(this);
      }
    }
  }
}
