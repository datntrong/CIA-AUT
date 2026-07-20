package uet.fit.server.rest.resource;

import org.jetbrains.annotations.NotNull;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.StreamingOutput;
import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public final class Utils {
	private Utils() {
	}

	static @NotNull String exceptionToString(@NotNull Exception exception) {
		final StringWriter writer = new StringWriter();
		try (final PrintWriter printWriter = new PrintWriter(writer)) {
			exception.printStackTrace(printWriter);
		}
		return writer.toString();
	}

	public static @NotNull String readResource(@NotNull String path) throws IOException {
		try (final InputStream inputStream = Utils.class.getResourceAsStream(path)) {
			if (inputStream == null) {
				throw new FileNotFoundException("Resource not found! path = " + path);
			}
			return new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
		}
	}

	public static @NotNull Response zipFolder(@NotNull String path) throws IOException {
		return Response.ok((StreamingOutput) output -> {
			final Path rootPath = Path.of(path).toRealPath();
			try (final ZipOutputStream outputStream = new ZipOutputStream(output)) {
				zipFile(rootPath, rootPath, outputStream);
			}
		}).header("Content-Disposition", "attachment; filename=\"logs.zip\"").build();
	}

	private static void zipFile(@NotNull Path rootPath, @NotNull Path path, @NotNull ZipOutputStream outputStream)
			throws IOException {
		if (Files.isDirectory(path)) {
//			outputStream.putNextEntry(new ZipEntry(path.getFileName().toString() + "/"));
//			outputStream.closeEntry();
			try (final DirectoryStream<Path> directoryStream = Files.newDirectoryStream(path)) {
				for (final Path child : directoryStream) {
					zipFile(rootPath, child, outputStream);
				}
			}
		} else if (Files.isRegularFile(path)) {
			outputStream.putNextEntry(new ZipEntry(rootPath.relativize(path).toString()));
			try (final InputStream inputStream = new BufferedInputStream(Files.newInputStream(path))) {
				inputStream.transferTo(outputStream);
			}
			outputStream.closeEntry();
		}
	}

}
