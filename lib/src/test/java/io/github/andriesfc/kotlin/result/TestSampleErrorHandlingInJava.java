package io.github.andriesfc.kotlin.result;

import static io.github.andriesfc.kotlin.result.ResultOperations.errorOrEmpty;
import static io.github.andriesfc.kotlin.result.ResultOperations.flatMap;
import static io.github.andriesfc.kotlin.result.ResultOperations.result;
import static io.github.andriesfc.kotlin.result.ResultOperations.success;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class TestSampleErrorHandlingInJava {

	private static Result<IOException, Long> fileSizeOf(File file) {
		return result(IOException.class, () -> {

			if (!file.exists()) {
				throw new FileNotFoundException(String.format("%s", file));
			}

			if (!file.isFile()) {
				BasicFileAttributes attributes = Files.readAttributes(file.toPath(), BasicFileAttributes.class);
				String fileType;
				if (attributes.isDirectory()) {
					fileType = "Directory";
				} else {
					fileType = "Other";
				}
				throw new IOException(String.format("Expected regular file, but instead \"%s\" found here: [%s].", fileType, file));
			}

			return success(file.length());
		});
	}

	private static Result<IOException,File> createDir(File file) {
		return result(IOException.class, () -> {
			if (!file.mkdirs()) {
				throw new IOException(String.format("Unable to create directory: %s", file));
			}
			return success(file);
		});
	}

	private static Result<IOException,File> createAnyNonEmptyFile(File file, int requiredFileByteSize) {
		return result(IOException.class, () -> {

			if (requiredFileByteSize < 0) {
				throw new IllegalArgumentException(String.format(
						"Required file size (arg 2) must be greater zero or greater, instead of %d", requiredFileByteSize));
			}

			if (file.exists()) {
				throw new IOException(String.format("Unable to create file, as it exists: %s", file));
			}

			if (!file.createNewFile()) {
				throw new IOException(String.format("Unable to create file: %s", file));
			}

			try(FileOutputStream f = new FileOutputStream(file)) {
				byte[] buffer = new byte[requiredFileByteSize];
				new Random().nextBytes(buffer);
				f.write(buffer);
				f.flush();
			}

			return success(file);
		});
	}

	@Test
	void testFileSizeResultHandlingOnNonExistingFile() {

		final File nonExistingFile = new File(String.format("%s.dat", UUID.randomUUID()));
		final Result<IOException, Long> fileSize = fileSizeOf(nonExistingFile);
		final Optional<IOException> fileSizeError = errorOrEmpty(fileSize);

		assertTrue(fileSizeError.isPresent());
		assertEquals(nonExistingFile.getName(), fileSizeError.get().getMessage());
		assertThrows(FileNotFoundException.class, fileSize::get);

	}

	@Test
	void testFileSizeResultHandlingOnDirectory(@TempDir File tempDir) {

		final Result<IOException, File> dir = createDir(new File(tempDir, String.format("dir-%s", UUID.randomUUID())));
		final Result<IOException,Long> fileSize = flatMap(dir, TestSampleErrorHandlingInJava::fileSizeOf);
		final Optional<IOException> fileSizeError = errorOrEmpty(fileSize);

		fileSizeError.ifPresent(System.out::println);
		assertTrue(fileSizeError.isPresent());
		final IOException actual = assertThrows(IOException.class, fileSize::get);
		assertNotNull(actual.getMessage());
		assertTrue(actual.getMessage().toLowerCase().startsWith("expected regular file"));
	}

	@Test
	void testGetFileSizeSucceeds(@TempDir File tempDir) {

		final int expectedFileSize = 1 + (new Random().nextInt(1023));
		Result<IOException, File> nonEmptyFile = createAnyNonEmptyFile(new File(tempDir,"just-some-non-empty-file"), expectedFileSize);

		System.out.printf("nonEmptyFile: %s%n", nonEmptyFile);
		assertFalse(errorOrEmpty(nonEmptyFile).isPresent());
		assertEquals(expectedFileSize, nonEmptyFile.get().length());
	}
}
