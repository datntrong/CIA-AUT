package uet.fit.cia.cpp.display;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.Serializable;

public final class ModificationsResponse implements Serializable {
	private @NotNull Modification @Nullable [] modifications;


	public @NotNull Modification @Nullable [] getModifications() {
		return modifications;
	}

	public void setModifications(@NotNull Modification @Nullable [] modifications) {
		this.modifications = modifications;
	}


	public static final class Modification implements Serializable {
		private @Nullable String qualifiedName;
		private @Nullable String withPrototype;
		private @NotNull String @Nullable [] filePaths;
		private @Nullable Change change;

		public @Nullable String getQualifiedName() {
			return qualifiedName;
		}

		public void setQualifiedName(@Nullable String qualifiedName) {
			this.qualifiedName = qualifiedName;
		}

		public @Nullable String getWithPrototype() {
			return withPrototype;
		}

		public void setWithPrototype(@Nullable String withPrototype) {
			this.withPrototype = withPrototype;
		}

		public @NotNull String @Nullable [] getFilePaths() {
			return filePaths;
		}

		public void setFilePaths(@NotNull String @Nullable [] filePaths) {
			this.filePaths = filePaths;
		}

		public @Nullable Change getChange() {
			return change;
		}

		public void setChange(@Nullable Change change) {
			this.change = change;
		}
	}

	public enum Change {
		ADDED,
		CHANGED,
		UNCHANGED,
		REMOVED
	}
}
