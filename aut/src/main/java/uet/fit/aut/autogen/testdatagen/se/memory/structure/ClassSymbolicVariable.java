package uet.fit.aut.autogen.testdatagen.se.memory.structure;

import uet.fit.aut.autogen.testdatagen.se.memory.PhysicalCell;

import java.util.ArrayList;
import java.util.List;

public class ClassSymbolicVariable extends SimpleStructureSymbolicVariable {

	public ClassSymbolicVariable(String name, String type, int scopeLevel) {
		super(name, type, scopeLevel);
	}

	@Override
	public List<PhysicalCell> getAllPhysicalCells() {
		// TODO: Get all physical cells of this symbolic variable
		return new ArrayList<>();
	}
}
