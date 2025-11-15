package org.eclipse.rdf4j.tools.serverboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, classes = Rdf4jServerWorkbenchApplication.class)
@ActiveProfiles("test")
class Rdf4jServerWorkbenchApplicationTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void serverRepositoriesEndpointResponds() {
		ResponseEntity<String> response = restTemplate.getForEntity(
				"http://localhost:" + port + "/rdf4j-server/repositories", String.class);

		assertThat(response.getStatusCode()).as("HTTP status for /rdf4j-server/repositories")
				.isEqualTo(HttpStatus.OK);
	}

	@Test
	void workbenchRootReturnsHtml() {
		ResponseEntity<String> redirect = restTemplate.getForEntity(
				"http://localhost:" + port + "/rdf4j-workbench/", String.class);

		assertThat(redirect.getStatusCode()).as("Redirect status for /rdf4j-workbench/")
				.isEqualTo(HttpStatus.FOUND);
		assertThat(redirect.getHeaders().getLocation()).as("Workbench redirect location")
				.isNotNull()
				.hasToString("http://localhost:" + port + "/rdf4j-workbench/repositories");

		ResponseEntity<String> response = followRedirects(redirect.getHeaders().getLocation());

		assertThat(response.getStatusCode()).as("HTTP status for /rdf4j-workbench/repositories")
				.isEqualTo(HttpStatus.OK);
		assertThat(response.getHeaders().getContentType()).as("Workbench content type")
				.isNotNull()
				.satisfies(mediaType -> assertThat(mediaType.toString())
						.contains("application/sparql-results+xml"));
		assertThat(response.getBody()).as("Workbench XML body")
				.contains("<?xml")
				.contains("<sparql");
	}

	private ResponseEntity<String> followRedirects(URI initialLocation) {
		assertThat(initialLocation).as("Initial redirect location").isNotNull();

		URI next = ensureAbsolute(initialLocation);
		ResponseEntity<String> current = restTemplate.getForEntity(next, String.class);
		int redirectAttempts = 0;
		while (current.getStatusCode().is3xxRedirection() && redirectAttempts < 5) {
			URI target = current.getHeaders().getLocation();
			assertThat(target).as("Redirect hop " + redirectAttempts).isNotNull();
			next = ensureAbsolute(target);
			current = restTemplate.getForEntity(next, String.class);
			redirectAttempts++;
		}
		return current;
	}

	private URI ensureAbsolute(URI uri) {
		if (uri.isAbsolute()) {
			return uri;
		}
		return URI.create("http://localhost:" + port).resolve(uri);
	}
}
