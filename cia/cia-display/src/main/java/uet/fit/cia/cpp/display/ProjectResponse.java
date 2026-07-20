package uet.fit.cia.cpp.display;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public final class ProjectResponse implements Serializable {
	private int fileRoot;
	private int languageRoot;
	private @NotNull Element @Nullable [] elements;


	public int getFileRoot() {
		return fileRoot;
	}

	public void setFileRoot(int fileRoot) {
		this.fileRoot = fileRoot;
	}

	public int getLanguageRoot() {
		return languageRoot;
	}

	public void setLanguageRoot(int languageRoot) {
		this.languageRoot = languageRoot;
	}

	public @NotNull Element @Nullable [] getElements() {
		return elements;
	}

	public void setElements(@NotNull Element @Nullable [] elements) {
		this.elements = elements;
	}


	public static final class Element implements Serializable {
		private @Nullable String name;
		private @Nullable Type type;
		private @Nullable Change change;
		private int @Nullable [] children;
		private @NotNull Dependency @Nullable [] dependencies;
		private @NotNull Location @Nullable [] locations;
		private double impact;

		public @Nullable String getName() {
			return name;
		}

		public void setName(@Nullable String name) {
			this.name = name;
		}

		public @Nullable Type getType() {
			return type;
		}

		public void setType(@Nullable Type type) {
			this.type = type;
		}

		public @Nullable Change getChange() {
			return change;
		}

		public void setChange(@Nullable Change change) {
			this.change = change;
		}

		public int @Nullable [] getChildren() {
			return children;
		}

		public void setChildren(int @Nullable [] children) {
			this.children = children;
		}

		public @NotNull Dependency @Nullable [] getDependencies() {
			return dependencies;
		}

		public void setDependencies(@NotNull Dependency @Nullable [] dependencies) {
			this.dependencies = dependencies;
		}

		public @NotNull Location @Nullable [] getLocations() {
			return locations;
		}

		public void setLocations(@NotNull Location @Nullable [] locations) {
			this.locations = locations;
		}

		public double getImpact() {
			return Math.round(impact * 100.0) / 100.0;
		}

		public void setImpact(float impact) {
			this.impact = impact;
		}
	}

	public enum Type {
		ROOT,
		DIRECTORY,
		FILE,
		CLASS,
		ENUM,
		FUNCTION,
		NAMESPACE,
		STRUCT,
		TYPEDEF,
		UNION,
		VARIABLE
	}

	public enum Change {
		ADDED,
		CHANGED,
		UNCHANGED,
		REMOVED
	}

	public static final class Dependency implements Serializable {
		private int target;
		private @NotNull Count @Nullable [] counts;

		public int getTarget() {
			return target;
		}

		public void setTarget(int target) {
			this.target = target;
		}

		public @NotNull Count @Nullable [] getCounts() {
			return counts;
		}

		public void setCounts(@NotNull Count @Nullable [] counts) {
			this.counts = counts;
		}
	}

	public static final class Count implements Serializable {
		private @Nullable String type;
		private int count;

		public @Nullable String getType() {
			return type;
		}

		public void setType(@Nullable String type) {
			this.type = type;
		}

		public int getCount() {
			return count;
		}

		public void setCount(int count) {
			this.count = count;
		}
	}

	public static final class Location implements Serializable {
		private @Nullable Change change;
		private @Nullable Position oldPosition;
		private @Nullable Position newPosition;

		public @Nullable Change getChange() {
			return change;
		}

		public void setChange(@Nullable Change change) {
			this.change = change;
		}

		public @Nullable Position getOldPosition() {
			return oldPosition;
		}

		public void setOldPosition(@Nullable Position oldPosition) {
			this.oldPosition = oldPosition;
		}

		public @Nullable Position getNewPosition() {
			return newPosition;
		}

		public void setNewPosition(@Nullable Position newPosition) {
			this.newPosition = newPosition;
		}
	}

	public static final class Position implements Serializable {
		private int file;
		private int startLine;
		private int endLine;

		public int getFile() {
			return file;
		}

		public void setFile(int file) {
			this.file = file;
		}

		public int getStartLine() {
			return startLine;
		}

		public void setStartLine(int startLine) {
			this.startLine = startLine;
		}

		public int getEndLine() {
			return endLine;
		}

		public void setEndLine(int endLine) {
			this.endLine = endLine;
		}
	}
}
