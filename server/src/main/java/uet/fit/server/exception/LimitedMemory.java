package uet.fit.server.exception;

public class LimitedMemory extends Exception {

	public LimitedMemory(long bytes) {
		super(String.format("There is not enough memory to handle a project sized %s", bytesIntoHumanReadable(bytes)));
	}

	private static String bytesIntoHumanReadable(long bytes) {
		long kilobyte = 1024;
		long megabyte = kilobyte * 1024;
		long gigabyte = megabyte * 1024;
		long terabyte = gigabyte * 1024;

		if ((bytes >= kilobyte) && (bytes < megabyte)) {
			return (bytes / kilobyte) + "kb";
		} else if ((bytes >= megabyte) && (bytes < gigabyte)) {
			return (bytes / megabyte) + "mb";
		} else if ((bytes >= gigabyte) && (bytes < terabyte)) {
			return (bytes / gigabyte) + "gb";
		} else if (bytes >= terabyte) {
			return (bytes / terabyte) + "tb";
		} else {
			return bytes + "bytes";
		}
	}
}
