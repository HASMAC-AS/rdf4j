package org.eclipse.rdf4j.server.boot;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.file.Path;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class Rdf4jServerBootApplicationTests {

	@TempDir
	static Path rdf4jHome;

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@DynamicPropertySource
	static void bootProperties(DynamicPropertyRegistry registry) {
		registry.add("rdf4j.boot.home", () -> rdf4jHome.toString());
		registry.add("rdf4j.boot.webapps[0].context-path", () -> "/rdf4j-server");
		registry.add("rdf4j.boot.webapps[0].location", () -> "classpath:test-webapps/rdf4j-server");
		registry.add("rdf4j.boot.webapps[1].context-path", () -> "/rdf4j-workbench");
		registry.add("rdf4j.boot.webapps[1].location", () -> "classpath:test-webapps/rdf4j-workbench");
	}

	@Test
	void serverHomePageIsServed() {
		ResponseEntity<String> response = restTemplate.getForEntity("http://localhost:" + port + "/rdf4j-server/",
				String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).contains("RDF4J Server");
	}

	@Test
	void workbenchHomePageIsServed() {
		ResponseEntity<String> response = restTemplate
				.getForEntity("http://localhost:" + port + "/rdf4j-workbench/", String.class);
		assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
		assertThat(response.getBody()).contains("RDF4J Workbench");
	}
}
