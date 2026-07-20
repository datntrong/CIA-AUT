package uet.fit.aut.testdata.comparable;

import uet.fit.aut.testdata.object.ValueDataNode;

import static uet.fit.aut.testdata.comparable.ValueStatementGenerator.getExportExeResultStmByType;

public class EqualityStatementGenerator extends StatementGenerator implements IEqualityComparable {

    public EqualityStatementGenerator(ValueDataNode node) {
        super(node);
    }

    @Override
    public String assertEqual(String source, String target) {
        StringBuilder assertion = new StringBuilder();

        if (haveValue()) {
            String expectedName = getVirtualName();
            String actualName = expectedName.replace(source, target);
            String resultExportStm = getExportExeResultStmByType(getRealType(), actualName, expectedName);
            assertion.append(resultExportStm);
        }

        return assertion.toString();
    }

    @Override
    public String assertNotEqual(String source, String target) {
        StringBuilder assertion = new StringBuilder();

        if (haveValue()) {
            String expectedName = getVirtualName();
            String actualName = expectedName.replace(source, target);
            String resultExportStm = getExportExeResultStmByType(getRealType(), actualName, expectedName);
            assertion.append(resultExportStm);
        }

        return assertion.toString();
    }

    protected boolean haveValue() {
        return node.haveValue();
    }
}
