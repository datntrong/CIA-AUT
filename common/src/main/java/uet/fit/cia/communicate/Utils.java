package uet.fit.cia.communicate;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.StringJoiner;
import java.util.stream.StreamSupport;

public final class Utils {
	private Utils() {
	}

	public static @NotNull String @NotNull [] fromPath(@NotNull Path path) {
		return StreamSupport.stream(path.spliterator(), false).map(Path::toString).toArray(String[]::new);
	}

	public static @NotNull Path toPath(@NotNull Path rootPath, @NotNull String @NotNull [] strings) {
		Path path = rootPath;
		for (final String string : strings) {
			path = path.resolve(string);
		}
		return path;
	}

	public static @NotNull String obfuscatePath(@NotNull Path path) {
		final StringJoiner joiner = new StringJoiner("/");
		for (final Path innerPath : path) {
			joiner.add(Integer.toHexString(innerPath.toString().hashCode()));
		}
		return joiner.toString();
	}
}
