package uet.fit.aut.parser.obj;

import org.eclipse.cdt.core.dom.ast.IASTCompositeTypeSpecifier;
import org.eclipse.cdt.core.dom.ast.IASTFileLocation;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTTemplateDeclaration;

import java.io.File;

public class ClassNode extends StructOrClassNode implements ISourceNavigable {

	@Override
	public String getNewType() {
		return ((IASTCompositeTypeSpecifier) getAST().getDeclSpecifier()).getName().toString();
	}

	@Override
	public IASTFileLocation getNodeLocation() {
		return ((IASTCompositeTypeSpecifier) getAST().getDeclSpecifier()).getName().getFileLocation();
	}

	@Override
	public File getSourceFile() {
		return new File(getAST().getContainingFilename());
	}

	public boolean isTemplate() {
		return getAST().getParent() instanceof ICPPASTTemplateDeclaration;
	}

	public IASTCompositeTypeSpecifier getSpecifiedAST() {
		return ((IASTCompositeTypeSpecifier) getAST().getDeclSpecifier());
	}

	@Override
	public String toString() {
		return this.getNewType();
	}

	@Override
	public INode clone() {
		ClassNode clone = new ClassNode();
		clone.setAST(getAST());
		clone.setName(getName());
		clone.setAbsolutePath(getAbsolutePath());
		clone.setParent(getParent());
		return clone;
	}
}
