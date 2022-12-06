package dev.bluebiscuitdesign.cucumber.dart;

import com.intellij.lang.LighterAST;
import com.intellij.lang.LighterASTNode;
import com.intellij.psi.impl.source.tree.LightTreeUtil;
import com.intellij.psi.impl.source.tree.RecursiveLighterASTNodeWalkingVisitor;
import com.intellij.util.indexing.DefaultFileTypeSpecificInputFilter;
import com.intellij.util.indexing.FileBasedIndex;
import com.intellij.util.indexing.ID;
import com.jetbrains.lang.dart.DartFileType;
import com.jetbrains.lang.dart.DartTokenTypes;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class DartCucumberIndex extends CucumberStepIndex {
    public static final ID<Boolean, List<Integer>> INDEX_ID = ID.create("dart.cucumber.step");

    @Override
    protected String[] getPackagesToScan() {
        return new String[] {"package:ogurets/ogurets.dart"};
    }

    @Override
    protected List<Integer> getAllStepDefinitionCalls(@NotNull LighterAST lighterAst, @NotNull CharSequence text) {
        List<Integer> result = new ArrayList<>();

        RecursiveLighterASTNodeWalkingVisitor visitor = new RecursiveLighterASTNodeWalkingVisitor(lighterAst) {
            @Override
            public void visitNode(@NotNull LighterASTNode element) {
                if (element.getTokenType() == DartTokenTypes.METADATA) {
                    List<LighterASTNode> methodNameAndArgumentList = lighterAst.getChildren(element);
                    if (methodNameAndArgumentList.size() < 3) {
                        super.visitNode(element);
                        return;
                    }
                    LighterASTNode methodNameNode = methodNameAndArgumentList.get(1);
                    if (methodNameNode != null && isStepDefinitionCall(methodNameNode, text)) {
                        LighterASTNode expressionList = methodNameAndArgumentList.get(2);
                        if (expressionList.getTokenType() == DartTokenTypes.ARGUMENTS) {
                            LighterASTNode argumentList = LightTreeUtil.firstChildOfType(lighterAst, expressionList, DartTokenTypes.ARGUMENT_LIST);
                            if (argumentList != null) {
                                LighterASTNode expressionParameter = LightTreeUtil.firstChildOfType(lighterAst, argumentList, DartTokenTypes.STRING_LITERAL_EXPRESSION);
                                if (expressionParameter != null) {
                                    result.add(expressionParameter.getStartOffset());
                                }
                            }
                        }
                    }
                }
                super.visitNode(element);
            }
        };
        visitor.visitNode(lighterAst.getRoot());

        return result;
    }

    @NotNull
    @Override
    public ID<Boolean, List<Integer>> getName() {
        return INDEX_ID;
    }

    @Override
    public int getVersion() {
        return 2;
    }

    @NotNull
    @Override
    public FileBasedIndex.InputFilter getInputFilter() {
        return new DefaultFileTypeSpecificInputFilter(DartFileType.INSTANCE);
    }
}
