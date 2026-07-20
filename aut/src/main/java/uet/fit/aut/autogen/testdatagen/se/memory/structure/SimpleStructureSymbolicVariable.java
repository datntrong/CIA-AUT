package uet.fit.aut.autogen.testdatagen.se.memory.structure;

import uet.fit.aut.autogen.testdatagen.se.memory.*;
import uet.fit.aut.autogen.testdatagen.testdatainit.VariableTypes;
import uet.fit.aut.config.IFunctionConfig;
import uet.fit.aut.logger.AUTLogger;
import uet.fit.aut.parser.obj.*;

import java.util.ArrayList;
import java.util.List;

public abstract class SimpleStructureSymbolicVariable extends SymbolicVariable {
	final static AUTLogger logger = AUTLogger.get(SimpleStructureSymbolicVariable.class);

	// Represent attributes in the structure variable
	protected List<ISymbolicVariable> attributes = new ArrayList<>();

	public SimpleStructureSymbolicVariable(String name, String type, int scopeLevel) {
		super(name, type, scopeLevel);
	}

	public List<ISymbolicVariable> getAttributes() {
		return attributes;
	}

	@Override
	public void setNode(INode node) {
		super.setNode(node);
		if (node instanceof StructureNode) {
			StructureNode cast = (StructureNode) node;
			for (IVariableNode attribute : cast.getAttributes()) {
				ISymbolicVariable symbolicAttribute = createSymbolicVariableFromAttribute(attribute);
				this.getAttributes().add(symbolicAttribute);
			}
		}
	}

	@Override
	public boolean assign(ISymbolicVariable other) {
		if (!(other.getClass().equals(getClass())))
			return false;

		SimpleStructureSymbolicVariable classVar = (SimpleStructureSymbolicVariable) other;

		if (!classVar.type.equals(type))
			return false;

		attributes.clear();
		attributes.addAll(classVar.attributes);

		return true;
	}

	public void setAttributes(List<ISymbolicVariable> attributes) {
		this.attributes = attributes;
	}

	protected ISymbolicVariable createSymbolicVariableFromAttribute(IVariableNode attribute) {
		SymbolicVariable v = null;

		// All passing variables have global access
		VariableNode par = (VariableNode) attribute;
		INode nodeType = par.resolveCoreType();
		String name = par.getName();
		String defaultValue = PREFIX_SYMBOLIC_VALUE + this.getName() + SEPARATOR_BETWEEN_STRUCTURE_NAME_AND_ITS_ATTRIBUTES
				+ name;

//        String realType = Utils.getRealType(par.getReducedRawType(), par.getParent());
		String realType = par.getRealType();

		IFunctionConfig functionConfig = function == null ? null : function.getFunctionConfig();

		if (VariableTypes.isAuto(realType))
			logger.error("Does not support type of the passing variable is auto");
		else {
			v = SymbolicVariable.create(name, realType, nodeType, defaultValue, scopeLevel, functionConfig);
		}
//		/*
//		 * ----------------NUMBER----------------------
//		 */
//		if (VariableTypes.isNumBasic(realType))
//			v = new NumberSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE, defaultValue);
//		else if (VariableTypes.isNumOneDimension(realType)) {
//			v = new OneDimensionNumberSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			((OneDimensionSymbolicVariable) v).getBlock().setName(defaultValue);
//
//		} else if (VariableTypeUtils.isNumMultiDimension(realType)) {
//			v = new MultipleDimensionNumberSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			((MultipleDimensionNumberSymbolicVariable) v).getBlock().setName(defaultValue);
//
//		} else if (VariableTypeUtils.isNumPointer(realType)) {
//			v = new PointerNumberSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			((PointerNumberSymbolicVariable) v).getReference().getBlock().setName(defaultValue);
//
//		} else
//		/*
//		 * ----------------CHARACTER----------------------
//		 */
//		if (VariableTypes.isChBasic(realType))
//			v = new CharacterSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE, defaultValue);
//		else if (VariableTypes.isChOneDimension(realType)) {
//			v = new OneDimensionCharacterSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			((OneDimensionSymbolicVariable) v).getBlock().setName(defaultValue);
//
//		} else if (VariableTypeUtils.isChMultiDimension(realType)) {
//			v = new MultipleDimensionCharacterSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			((MultipleDimensionCharacterSymbolicVariable) v).getBlock().setName(defaultValue);
//
//		} else if (VariableTypeUtils.isChPointer(realType)) {
//			v = new PointerCharacterSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			((PointerCharacterSymbolicVariable) v).getReference().getBlock().setName(defaultValue);
//			((PointerCharacterSymbolicVariable) v)
//					.setSize(this.getFunction().getFunctionConfig().getBoundOfArray().getLower() + "");
//
//		} else
//		/*
//		 * ----------------STRUCTURE----------------------
//		 */
//		if (VariableTypes.isStructureSimple(realType)) {
//
//			if (nodeType instanceof UnionNode)
//				v = new UnionSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof StructNode)
//				v = new StructSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof ClassNode)
//				v = new ClassSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof EnumNode)
//				v = new EnumSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//
//		} else if (VariableTypes.isStructureOneDimension(realType)) {
//			if (nodeType instanceof UnionNode)
//				v = new OneDimensionUnionSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof StructNode)
//				v = new OneDimensionStructSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof ClassNode)
//				v = new OneDimensionClassSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof EnumNode)
//				v = new OneDimensionEnumSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//
//			if (v != null)
//				((OneDimensionSymbolicVariable) v).getBlock().setName(defaultValue);
//
//		} else if (VariableTypeUtils.isStructureMultiDimension(realType)) {
//			if (nodeType instanceof UnionNode)
//				v = new MultipleDimensionUnionSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof StructNode)
//				v = new MultipleDimensionStructSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof ClassNode)
//				v = new MultipleDimensionUnionSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof EnumNode)
//				v = new MultipleDimensionEnumSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//
//			if (v != null)
//				((ArraySymbolicVariable) v).getBlock().setName(defaultValue);
//
//		} else if (VariableTypeUtils.isStructurePointerMultiLevel(realType)
//				|| VariableTypeUtils.isStructureOneLevel(realType)) {
//			if (nodeType instanceof UnionNode)
//				v = new PointerUnionSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof StructNode)
//				v = new PointerStructSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof ClassNode)
//				v = new PointerClassSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//			else if (nodeType instanceof EnumNode)
//				v = new PointerEnumSymbolicVariable(name, realType, ISymbolicVariable.GLOBAL_SCOPE);
//
//			if (v != null)
//				((PointerSymbolicVariable) v).getReference().getBlock().setName(defaultValue);
//		}

		return v;
	}
}
