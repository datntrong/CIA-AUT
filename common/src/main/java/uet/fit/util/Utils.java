package uet.fit.util;

import org.jetbrains.annotations.NotNull;

public class Utils {

	@NotNull
	public static String shortenCommitHash(@NotNull String hash) {
		return hash.substring(0, 8);
	}

}
