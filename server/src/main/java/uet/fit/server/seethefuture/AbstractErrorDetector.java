package uet.fit.server.seethefuture;

import org.eclipse.cdt.internal.core.dom.parser.cpp.CPPASTCompoundStatement;
import uet.fit.aut.parser.obj.IFunctionNode;

public abstract class AbstractErrorDetector {

	private final IFunctionNode functionNode;

	public AbstractErrorDetector(IFunctionNode functionNode) {
		this.functionNode = functionNode;
	}

	abstract void detect();

	public static void main(String[] args) {
		System.out.println();

	}
}
