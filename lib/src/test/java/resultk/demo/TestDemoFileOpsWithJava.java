package resultk.demo;

import resultk.Result;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static resultk.ResultOperations.errorOrEmpty;
import static resultk.ResultOperations.flatmap;
import static org.junit.jupiter.api.Assertions.*;

class TestDemoFileOpsWithJava {

	@Test
	void testFileSizeResultHandlingOnNonExistingFile() {

		final File nonExistingFile = new File(String.format("%s.dat", UUID.randomUUID()));
		final Result<IOException, Long> fileSize = resultk.demo.DemoFileOpsKt.fileSize(nonExistingFile);
		final Optional<IOException> fileSizeError = errorOrEmpty(fileSize);

		assertTrue(fileSizeError.isPresent());
		assertEquals(nonExistingFile.getName(), fileSizeError.get().getMessage());
		assertThrows(FileNotFoundException.class, fileSize::get);

	}

	@Test
	void testFileSizeResultHandlingOnDirectory(@TempDir File tempDir) {

		final Result<IOException, File> dir = resultk.demo.DemoFileOpsKt.makeDir(new File(tempDir, String.format("dir-%s", UUID.randomUUID())));
		final Result<IOException, Long> fileSize = flatmap(dir, DemoFileOpsKt::fileSize);
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
		Result<IOException, File> nonEmptyFile = resultk.demo.DemoFileOpsKt.appendAnyBytes(new File(tempDir, "just-some-non-empty-file"), expectedFileSize);
		System.out.printf("nonEmptyFile: %s%n", nonEmptyFile);
		assertFalse(errorOrEmpty(nonEmptyFile).isPresent());
		assertEquals(expectedFileSize, nonEmptyFile.get().length());
	}
}
