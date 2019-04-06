package org.jetbrains.plugins.cucumber.dart.steps;

import com.google.common.primitives.Primitives;
import cucumber.runtime.snippets.Snippet;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;

public class DartSnippet implements Snippet {
  protected String getArgType(Class<?> argType) {
    return argType.isPrimitive() ? Primitives.wrap(argType).getSimpleName() : argType.getSimpleName();
  }

  @Override
  public String template() {
    return "@{0}(\"{1}\")\nvoid {2}({3}) async \'{\'\n    // {4}\n{5}    throw new PendingException();\n\'}\'\n";
  }

  @Override
  public String tableHint() {
    return "";
  }

  @Override
  public String arguments(List<Class<?>> argumentTypes) {
    StringBuilder result = new StringBuilder();
    for (int i = 0; i < argumentTypes.size(); i++) {
      Class<?> arg = argumentTypes.get(i);
      if (i > 0) {
        result.append(", ");
      }
      result.append(getArgType(arg)).append(" arg").append(i);
    }

    return result.toString();
  }

  @Override
  public String namedGroupStart() {
    return null;
  }

  @Override
  public String namedGroupEnd() {
    return null;
  }

  @Override
  public String escapePattern(String pattern) {
    return pattern.replace("\\", "\\\\").replace("\"", "\\\"");
  }
}
