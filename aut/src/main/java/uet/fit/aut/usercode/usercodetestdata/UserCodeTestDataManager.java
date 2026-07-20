package uet.fit.aut.usercode.usercodetestdata;

import java.util.HashMap;
import java.util.List;
import java.util.Random;

public class UserCodeTestDataManager {
	/**
	 * <nameArgument , listContentForArgument>
	 */
	public static String getFirstContentForArgument(HashMap<String, List<String>> userCode,String nameArgument) {
		List<String> userCodeContents = userCode.get(nameArgument);
		if (userCodeContents == null) {
			return "";
		}
		return userCodeContents.get(0);
	}

	public static String getRandomContentForArgument(HashMap<String, List<String>> userCode,String nameArgument) {
		List<String> userCodeContents = userCode.get(nameArgument);
		if (userCodeContents == null) {
			return "";
		}

		if (userCodeContents.size() == 1) {
			return userCodeContents.get(0);
		}

		int index = new Random().nextInt(userCodeContents.size());
		return userCodeContents.get(index);
	}
}
