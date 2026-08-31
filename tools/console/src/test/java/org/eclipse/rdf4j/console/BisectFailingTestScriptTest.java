package org.eclipse.rdf4j.console;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class BisectFailingTestScriptTest {

	@Test
	@DisplayName("bisect script must not invoke Maven with the quiet flag")
	void bisectScriptAvoidsQuietMavenFlag() throws IOException {
		Path script = locateScript(Path.of(""));
		String contents = Files.readString(script, StandardCharsets.UTF_8);

		assertThat(contents)
				.as("bisect-failing-test.sh should not rely on mvn -q, which hides required logs")
				.doesNotContain("mvn -q");
	}

	private Path locateScript(Path start) throws IOException {
		Path current = start.toAbsolutePath();
		while (current != null) {
			Path candidate = current.resolve("bisect-failing-test.sh");
			if (Files.isRegularFile(candidate)) {
				return candidate;
			}
			current = current.getParent();
		}
		throw new IOException("Could not find bisect-failing-test.sh from " + start);
	}
}
