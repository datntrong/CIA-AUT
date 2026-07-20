package uet.fit.aut.util;

import org.eclipse.cdt.core.dom.ast.*;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTPointerToMember;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTReferenceOperator;
import org.eclipse.cdt.core.parser.IToken;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ASTVisualizer {

	private static final Logger logger = LoggerFactory.getLogger(ASTVisualizer.class);

	public static String toString(IASTDeclSpecifier specifier) {
		String type;

		try {
			if (specifier instanceof IASTNamedTypeSpecifier) {
				type = ((IASTNamedTypeSpecifier) specifier).getName().toString();
				if (specifier.isConst())
					type = "const " + type;
			} else if (specifier instanceof IASTElaboratedTypeSpecifier) {
				String kind = SpecialCharacter.EMPTY;
				switch (((IASTElaboratedTypeSpecifier) specifier).getKind()) {
					case IASTElaboratedTypeSpecifier.k_last + 1:
						kind = "class ";
						break;

					case IASTElaboratedTypeSpecifier.k_enum:
						kind = "enum ";
						break;

					case IASTElaboratedTypeSpecifier.k_struct:
						kind = "struct ";
						break;

					case IASTElaboratedTypeSpecifier.k_union:
						kind = "union ";
						break;
				}
				String structure = ((IASTElaboratedTypeSpecifier) specifier).getName().toString();
				type = kind + structure;
				if (specifier.isConst())
					type = "const " + type;
			} else if (specifier instanceof IASTSimpleDeclSpecifier) {
				if (((IASTSimpleDeclSpecifier) specifier).getType() == IASTSimpleDeclSpecifier.t_auto) {
					type = specifier + " auto";
				} else {
					type = specifier.toString();
				}
			} else if (specifier instanceof IASTEnumerationSpecifier) {
				type = ((IASTEnumerationSpecifier) specifier).getName().toString();
				if (specifier.isConst())
					type = "const " + type;
			} else if (specifier instanceof IASTCompositeTypeSpecifier) {
				type = ((IASTCompositeTypeSpecifier) specifier).getName().toString();
				if (specifier.isConst())
					type = "const " + type;
			} else {
				type = specifier.getRawSignature();
			}
		} catch (Exception ex) {
			type = specifier.getRawSignature();
		}

		return type;
	}

	public static String toString(IASTDeclSpecifier specifier, IASTDeclarator declarator) {
		String type = toString(specifier);

		for (IASTPointerOperator op : declarator.getPointerOperators()) {
			if (op instanceof ICPPASTReferenceOperator) {
				try {
					IToken token = op.getSyntax();
					int len = token.getLength();
					for (int i = 0; i < len; i++)
						type += "&";
				} catch (ExpansionOverlapsBoundaryException e) {
					type += "&";
				}
			} else if (op instanceof IASTPointer) {
				if (op instanceof ICPPASTPointerToMember) {
					logger.error("Not support ICPPASTPointerToMember");
				} else {
					type += "*";
				}
			}
		}

		if (declarator instanceof IASTArrayDeclarator) {
			IASTArrayModifier[] modifiers = ((IASTArrayDeclarator) declarator).getArrayModifiers();
			for (IASTArrayModifier modifier : modifiers) {
				type += "[" + toString(modifier.getConstantExpression()) + "]";
			}
		}

		return type;
	}

	public static String toString(IASTInitializerClause expr) {
		if (expr == null)
			return SpecialCharacter.EMPTY;

		boolean containMacro = false;

		for (IASTNodeLocation location : expr.getNodeLocations()) {
			if (location instanceof IASTMacroExpansionLocation) {
				containMacro = true;
				break;
			}
		}

		if (containMacro)
			return SpecialCharacter.EMPTY;

		return expr.getRawSignature();
	}
}
