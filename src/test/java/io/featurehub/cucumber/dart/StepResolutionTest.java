package io.featurehub.cucumber.dart;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.testFramework.fixtures.BasePlatformTestCase;
import com.jetbrains.lang.dart.psi.DartComponent;
import org.jetbrains.plugins.cucumber.steps.reference.CucumberStepReference;

/**
 * Checks that steps in a .feature file resolve to Dart step definitions,
 * both as class methods and as top-level functions.
 */
public class StepResolutionTest extends BasePlatformTestCase {

    private static final String STEPS = """
            import 'package:ogurets/ogurets.dart';

            @Given("I have the following PyString:")
            void iHaveTheFollowingPystring(dynamic pyString) {
            }

            class Steps {
              @Given("the above Step should have the PyString as last parameter.")
              void theAboveStep() {
              }
            }
            """;

    private static final String FEATURE = """
            Feature: Supporting PyStrings

              Scenario: Un-indented PyStrings
                Given I have the following PyString:
            \"\"\"
            line 1
            \"\"\"
                Then the above Step should have the PyString as last parameter.
            """;

    public void testTopLevelFunctionStepResolves() {
        assertResolvesTo("I have the following", "iHaveTheFollowingPystring");
    }

    public void testClassMethodStepResolves() {
        assertResolvesTo("the above Step", "theAboveStep");
    }

    private void assertResolvesTo(String stepTextStart, String expectedFunction) {
        myFixture.addFileToProject("steps/py_strings.dart", STEPS);
        myFixture.configureByText("pystrings.feature", FEATURE);

        int offset = myFixture.getFile().getText().indexOf(stepTextStart) + 1;
        PsiReference ref = myFixture.getFile().findReferenceAt(offset);
        assertNotNull("no reference at step '" + stepTextStart + "'", ref);

        CucumberStepReference stepRef = ref instanceof CucumberStepReference c ? c : null;
        assertNotNull("not a step reference: " + ref.getClass(), stepRef);

        PsiElement resolved = stepRef.resolve();
        assertNotNull("step '" + stepTextStart + "' did not resolve", resolved);
        DartComponent owner = CucumberDartUtil.findStepDefinitionOwner(resolved);
        String name = resolved instanceof DartComponent dc ? dc.getName() : owner != null ? owner.getName() : resolved.getText();
        assertEquals(expectedFunction, name);
    }
}
