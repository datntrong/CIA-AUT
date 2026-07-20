package uet.fit.aut.autogen.testdatagen.se.solver;

import uet.fit.aut.autogen.testdatagen.AbstractAutomatedTestdataGeneration;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.util.SpecialCharacter;
import uet.fit.aut.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.Calendar;
import java.util.Date;

/**
 * Chạy file smt-lib trên cmd sử dụng SMT-Solver Z3
 *
 * @author anhanh
 */
public class RunZ3OnCMD {
    final static AUTLogger logger = AUTLogger.get(RunZ3OnCMD.class);

    private final String Z3Path;
    private final String smtLibPath;
    private String result = SpecialCharacter.EMPTY;

    public RunZ3OnCMD(String Z3Path, String smtLibPath) {
        this.Z3Path = Z3Path;
        this.smtLibPath = smtLibPath;
    }

    public synchronized void execute() throws Exception {
        logger.debug("RunZ3OnCMD begin");

        Date startTime = Calendar.getInstance().getTime();

        Process p = null;
        if (Utils.isWindows()) {
            p = Runtime.getRuntime().exec(
                    new String[]{Utils.doubleNormalizePath(Z3Path), "-smt2", smtLibPath}
//                    , new String[]{},
//                    new File(Z3Path).getParentFile()
            );
        } else if (Utils.isUnix()) {
            p = Runtime.getRuntime().exec(
                    new String[]{"./" + new File(Z3Path).getName(), "-smt2", smtLibPath}
                    , new String[]{},
                    new File(Z3Path).getParentFile());
        } else if (Utils.isMac()) {
            p = Runtime.getRuntime().exec(new String[]{Z3Path, "-smt2", smtLibPath});
        }

        assert p != null;
        p.waitFor();

        AbstractAutomatedTestdataGeneration.numOfSolverCalls++;
        Date end = Calendar.getInstance().getTime();
        AbstractAutomatedTestdataGeneration.solverRunningTime += end.getTime() - startTime.getTime();

        BufferedReader in = new BufferedReader(new InputStreamReader(p.getInputStream()));
        StringBuilder builder = new StringBuilder();
        String line;
        while ((line = in.readLine()) != null)
            builder.append(line).append(SpecialCharacter.LINE_BREAK);
        result = builder.toString();

        // Display errors if exists
        if (p.getErrorStream() != null) {
            BufferedReader error = new BufferedReader(new InputStreamReader(p.getErrorStream()));
            String err;
            boolean hasError = false;
            while ((err = error.readLine()) != null) {
                logger.error(err);
                hasError = true;
            }
            if (hasError)
                AbstractAutomatedTestdataGeneration.numOfSolverCallsbutCannotSolve++;
        }

        logger.debug("RunZ3OnCMD end");
    }

    public String getSolution() {
        return result;
    }
}
