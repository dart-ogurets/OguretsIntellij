package dev.bluebiscuitdesign.cucumber.dart.steps.snippets;

import cucumber.runtime.snippets.ArgumentPattern;
import cucumber.runtime.snippets.FunctionNameGenerator;
import io.cucumber.messages.types.PickleStep;
import io.cucumber.messages.types.PickleStepArgument;
import org.jetbrains.plugins.cucumber.psi.GherkinTable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SnippetGenerator {
    private static final ArgumentPattern[] DEFAULT_ARGUMENT_PATTERNS = new ArgumentPattern[]{
            new ArgumentPattern(Pattern.compile("([-+]?\\d+)"), "{int}", Integer.TYPE),
            new ArgumentPattern(Pattern.compile("([+-]?([0-9]*[.])?[0-9]+)"), "{float}", Float.TYPE),
			// new ArgumentPattern(Pattern.compile("([[-+]?\\d+|<\\w+?>])"), "{int}",Integer.TYPE),
			// new ArgumentPattern(Pattern.compile("([[-+]?[0-9]*\\.?[0-9]+|<\\w+?>])"), "{float}", Float.TYPE),
            new ArgumentPattern(Pattern.compile("\"([^\"]*)\""), "{string}", String.class),
            new ArgumentPattern(Pattern.compile("<([^>]*)>"), "{string}", String.class)
    };
    private static final Pattern GROUP_PATTERN = Pattern.compile("\\(");
    private static final Pattern[] ESCAPE_PATTERNS = new Pattern[]{
            Pattern.compile("\\$"),
            Pattern.compile("\\("),
            Pattern.compile("\\)"),
            Pattern.compile("\\["),
            Pattern.compile("\\]"),
            Pattern.compile("\\?"),
            Pattern.compile("\\*"),
            Pattern.compile("\\+"),
            Pattern.compile("\\."),
            Pattern.compile("\\^")
    };

    private static final String REGEXP_HINT = "Write code here that turns the phrase above into concrete actions";

    private final ParamSnippet snippet;

    public SnippetGenerator(ParamSnippet snippet) {
        this.snippet = snippet;
    }

    public String getSnippet(PickleStep step, String keyword, FunctionNameGenerator functionNameGenerator) {
        String regex = snippet.escapePattern(patternFor(step.getText()));
        String name = functionName(step.getText(), functionNameGenerator);
        String args = snippet.paramArguments(argumentTypes(step));
        String table = step.getArgument().isPresent() && step.getArgument().get().getDataTable().isPresent() ? snippet.tableHint() : "";
        return MessageFormat.format(
                snippet.template(),
                keyword,
                regex,
                name,
                args,
                REGEXP_HINT,
                table
        );
    }

    String patternFor(String stepName) {
        String pattern = stepName;
        for (Pattern escapePattern : ESCAPE_PATTERNS) {
            Matcher m = escapePattern.matcher(pattern);
            String replacement = Matcher.quoteReplacement(escapePattern.toString());
            pattern = m.replaceAll(replacement);
        }
        for (ArgumentPattern argumentPattern : argumentPatterns()) {
            pattern = argumentPattern.replaceMatchesWithGroups(pattern);
        }
        if (snippet.namedGroupStart() != null) {
            pattern = withNamedGroups(pattern);
        }

        return pattern;
    }

    private String functionName(String sentence, FunctionNameGenerator functionNameGenerator) {
        if (functionNameGenerator == null) {
            return null;
        }
        for (ArgumentPattern argumentPattern : argumentPatterns()) {
            sentence = argumentPattern.replaceMatchesWithSpace(sentence);
        }
        return functionNameGenerator.generateFunctionName(sentence);
    }


    private String withNamedGroups(String snippetPattern) {
        Matcher m = GROUP_PATTERN.matcher(snippetPattern);

        StringBuffer sb = new StringBuffer();
        int n = 1;
        while (m.find()) {
            m.appendReplacement(sb, "(" + snippet.namedGroupStart() + n++ + snippet.namedGroupEnd());
        }
        m.appendTail(sb);

        return sb.toString();
    }

    private List<ParamSnippet.ArgumentParam> argumentTypes(PickleStep step) {
        String name = step.getText();
        List<ParamSnippet.ArgumentParam> argTypes = new ArrayList<>();
        Matcher[] matchers = new Matcher[argumentPatterns().length];
        for (int i = 0; i < argumentPatterns().length; i++) {
            matchers[i] = argumentPatterns()[i].pattern().matcher(name);
        }
        int pos = 0;
        while (true) {
            int matchedLength = 1;

            for (int i = 0; i < matchers.length; i++) {
                Matcher m = matchers[i].region(pos, name.length());
                if (m.lookingAt()) {
                    Class<?> typeForSignature = argumentPatterns()[i].type();
                    matchedLength = m.group().length();
                    String pName = name.subSequence(pos, pos + matchedLength).toString();
                    final ParamSnippet.ArgumentParam.Builder param = new ParamSnippet.ArgumentParam.Builder().clazz(typeForSignature);
                    if (pName.startsWith("\"<")) {
                        param.name(pName.substring(2, pName.length() - 2));
                    } else if (pName.startsWith("<")) {
                        param.name(pName.substring(1, pName.length() - 1));
                    }
                    argTypes.add(param.build());
                    break;
                }
            }
            pos += matchedLength;
            if (pos == name.length()) {
                break;
            }
        }
        if (step.getArgument().isPresent()) {
            PickleStepArgument arg = step.getArgument().get();
            if (arg.getDataTable().isPresent()) {
                argTypes.add(new ParamSnippet.ArgumentParam.Builder().clazz(String.class).build());
            }
            if (arg.getDataTable().isPresent()) {
                argTypes.add(new ParamSnippet.ArgumentParam.Builder().clazz(GherkinTable.class).build());
            }
        }
        return argTypes;
    }

    ArgumentPattern[] argumentPatterns() {
        return DEFAULT_ARGUMENT_PATTERNS;
    }

    public static String untypedArguments(List<Class<?>> argumentTypes) {
        StringBuilder sb = new StringBuilder();
        for (int n = 0; n < argumentTypes.size(); n++) {
            if (n > 0) {
                sb.append(", ");
            }
            sb.append("arg").append(n + 1);
        }
        return sb.toString();
    }
}
