package uet.fit.aut.autogen.testdatagen.se.normalstatementparser;

import uet.fit.aut.autogen.testdatagen.se.ExpressionRewriterUtils;
import uet.fit.aut.autogen.testdatagen.se.memory.FunctionCallTable;
import uet.fit.aut.autogen.testdatagen.se.memory.VariableNodeTable;
import uet.fit.aut.util.IRegex;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;
import org.eclipse.cdt.core.dom.ast.IASTNode;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ConditionParser extends StatementParser {

    private String newConstraint = "";

    @Override
    public void parse(IASTNode ast, VariableNodeTable table, FunctionCallTable callTable) throws Exception {
        ast = Utils.shortenAstNode(ast);
        newConstraint = ExpressionRewriterUtils.rewrite(table, ast.getRawSignature());

//        //replace enum with normal index of array
//        List<String[]> enumItemList = new ArrayList<>();
//        //find all enum related to containing file
//        List<INode> uutList = Environment.getInstance().getUUTs();
//        List<Level> dependentFileLevel = new ArrayList<>();
//        for (int i = 0; i < uutList.size(); i++){
//            dependentFileLevel.addAll(new VariableSearchingSpace(uutList.get(i)).getSpaces());
//        }
//        for (Level fileLevel : dependentFileLevel) {
//            List<EnumNode> enumlist = Search.searchNodes(fileLevel.get(0), new EnumNodeCondition());
//            for (EnumNode enumNode : enumlist){
//                enumItemList.addAll(enumNode.getAllEnumItems());
//            }
//        }
//        for (String[] item : enumItemList) {
//            if (newConstraint.contains(item[0]))
//                newConstraint = newConstraint.replaceAll("\\b\\s*"+item[0]+"\\s*\\b", item[1]);
//        }
    }

    public String getNewConstraint() {
        return newConstraint;
    }

    private static String removeRedundantBrackets(String constraint) {
        String rewriteStm = constraint;

        String pattern = "\\b" + IRegex.OPENING_PARETHENESS + IRegex.SPACES
                + IRegex.NAME_REGEX + IRegex.SPACES + IRegex.CLOSING_PARETHENESS + "\\b";

        // Create a pattern object
        Pattern r = Pattern.compile(pattern);

        // Now create matcher object.
        Matcher m = r.matcher(rewriteStm);

        while (m.find()) {
            String fullExpr = m.group(0);
            String body = fullExpr.replaceAll(IRegex.OPENING_PARETHENESS, SpecialCharacter.EMPTY)
                    .replaceAll(IRegex.CLOSING_PARETHENESS, SpecialCharacter.EMPTY);
            rewriteStm = rewriteStm.replace(fullExpr, body);
            m = r.matcher(rewriteStm);
        }

        rewriteStm = rewriteStm.replaceAll("^" + IRegex.OPENING_PARETHENESS + IRegex.SPACES
                + "(" + IRegex.NAME_REGEX  + ")" + IRegex.SPACES + IRegex.CLOSING_PARETHENESS, "$1").trim();

        return rewriteStm;
    }

    public void setNewConstraint(String newConstraint) {
        this.newConstraint = newConstraint;
    }
}
