package uet.fit.aut.parser.dependency;

import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.IASTSimpleDeclaration;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTNewExpression;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uet.fit.aut.logger.IdMapping;
import uet.fit.aut.parser.FunctionCallParser;
import uet.fit.aut.parser.ProjectParser;
import uet.fit.aut.parser.finder.MethodFinder;
import uet.fit.aut.parser.obj.IFunctionNode;
import uet.fit.aut.parser.obj.INode;
import uet.fit.config.Config;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

public class FunctionCallDependencyGeneration extends AbstractDependencyGeneration<IFunctionNode> {

	private final static Logger logger = LoggerFactory.getLogger(FunctionCallDependencyGeneration.class);

	private static final ExecutorService es = ProjectParser.es; //AutExecutors.newWorkStealingPool();

	private MethodFinder finder;

	public void dependencyGeneration(IFunctionNode root) {
		if (Config.isFunctionCallAnalyze()) {
			if (!root.isFunctionCallDependencyState()) {
				finder = new MethodFinder(root);

				IASTFunctionDefinition fnAst = root.getAST();

				FunctionCallParser visitor = new FunctionCallParser();
				fnAst.accept(visitor);

				try {
					checkExpectedList(visitor, root);
					checkUnExpectedList(visitor, root);
				} catch (InterruptedException | ExecutionException e) {
					logger.error("Can't run parallel to analyze function call dependency", e);
				}

				root.setFunctionCallDependencyState(true);
			} else {
				logger.debug(IdMapping.getInstance().getOrCreate(root.getAbsolutePath()) + " is analyzed function call dependency before");
			}
		} else {
			logger.debug("Ignore function call dependency analyzer");
		}
	}

	private synchronized void addDependency(INode owner, INode referredNode) {
		FunctionCallDependency d = new FunctionCallDependency(owner, referredNode);

		synchronized (owner.getDependencies()) {
			if (!owner.getDependencies().contains(d)) {
				owner.getDependencies().add(d);
			}
		}

		synchronized (referredNode.getDependencies()) {
			if (!referredNode.getDependencies().contains(d)) {
				referredNode.getDependencies().add(d);
			}
		}

		logger.debug("Found a function call dependency: " + d);

//        if (!owner.getDependencies().contains(d)
//                && !referredNode.getDependencies().contains(d)) {
//            owner.getDependencies().add(d);
//            referredNode.getDependencies().add(d);
//            logger.debug("Found a function call dependency: " + d);
//        }
	}

	private void checkExpectedList(FunctionCallParser visitor, IFunctionNode owner) throws InterruptedException, ExecutionException {
		List<IASTExpression> expressions = visitor.getExpressions();

		List<Callable<INode>> findTasks = new ArrayList<>();

		for (IASTExpression expression : expressions) {
			Callable<INode> findTask = null;

			if (expression instanceof IASTFunctionCallExpression) {
				findTask = new Callable<INode>() {
					@Override
					public INode call() throws Exception {
						return finder.find((IASTFunctionCallExpression) expression);
					}
				};
			} else if (expression instanceof ICPPASTNewExpression) {
				findTask = new Callable<INode>() {
					@Override
					public INode call() throws Exception {
						return finder.find((ICPPASTNewExpression) expression);
					}
				};
			}

			if (findTask != null) findTasks.add(findTask);
		}

		List<Future<INode>> futures = es.invokeAll(findTasks);
		for (Future<INode> future : futures) {
			INode referredNode = future.get();
			if (referredNode != null)
				addDependency(owner, referredNode);
		}
//		es.shutdown();
	}

	private void checkUnExpectedList(FunctionCallParser visitor, IFunctionNode owner) throws InterruptedException, ExecutionException {
		List<IASTSimpleDeclaration> names = visitor.getUnexpectedCalledFunctions();

		List<Callable<List<INode>>> findTasks = new ArrayList<>();

		for (IASTSimpleDeclaration name : names) {
			String funcName = name.getDeclarators()[0].getName().toString();

			// ignore the first children because it is corresponding to the name of the called method
			int nParameters = name.getChildren().length - 1;

			Callable<List<INode>> findTask = new Callable<List<INode>>() {
				@Override
				public List<INode> call() throws Exception {
					return finder.find(funcName, nParameters);
				}
			};

			findTasks.add(findTask);
		}

		List<Future<List<INode>>> futures = es.invokeAll(findTasks);
		for (Future<List<INode>> future : futures) {
			List<INode> referredNodes = future.get();
			for (INode referredNode : referredNodes) {
				addDependency(owner, referredNode);
			}
		}
//		es.shutdown();
	}
}

