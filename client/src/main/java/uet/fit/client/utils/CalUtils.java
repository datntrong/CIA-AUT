package uet.fit.client.utils;

import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CalUtils {
	public static double round(double value, int precision) {
		int scale = (int) Math.pow(10, precision);
		return (double) Math.round(value * scale) / scale;
	}

	public static boolean validateName(@NotNull String name) {
		// Regex to check valid env name.
		String regex = "^[A-Za-z]\\w*$";

		// Compile the ReGex
		Pattern p = Pattern.compile(regex);

		// Pattern class contains matcher() method
		// to find matching between given username
		// and regular expression.
		Matcher m = p.matcher(name);

		// Return if the username
		// matched the ReGex
		return m.matches() && name.length() <= 32;
	}

}
