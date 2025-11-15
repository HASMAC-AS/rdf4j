package org.eclipse.rdf4j.serverboot;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = "spring.main.allow-bean-definition-overriding=true")
class EmbeddedServerBootTest {

	@LocalServerPort
	private int port;

	@Autowired
	private TestRestTemplate restTemplate;

	@Test
	void serverRepositoriesEndpointIsExposed() {
		ResponseEntity<String> response = restTemplate
				.getForEntity("http://localhost:" + port + "/rdf4j-server/repositories", String.class);
		assertThat(response.getStatusCode().is2xxSuccessful())
				.as("/rdf4j-server/repositories should be served by embedded Tomcat")
				.isTrue();
	}

	@Test
	void workbenchRepositoriesPageIsServed() {
		String workbenchUrl = "http://localhost:" + port + "/rdf4j-workbench/repositories";
		HttpHeaders headers = new HttpHeaders();
		headers.setAccept(Arrays.asList(MediaType.TEXT_HTML, MediaType.ALL));
		HttpEntity<Void> requestEntity = new HttpEntity<>(headers);
		ResponseEntity<String> response = restTemplate.exchange(workbenchUrl, HttpMethod.GET, requestEntity,
				String.class);
		if (response.getStatusCode().is3xxRedirection()) {
			String redirect = response.getHeaders().getLocation() != null
					? response.getHeaders().getLocation().toString()
					: response.getHeaders().getFirst("Location");
			assertThat(redirect).as("Workbench redirect should provide a Location header").isNotBlank();
			if (!redirect.startsWith("http")) {
				if (!redirect.startsWith("/")) {
					redirect = "/" + redirect;
				}
				redirect = "http://localhost:" + port + redirect;
			}
			response = restTemplate.exchange(redirect, HttpMethod.GET, requestEntity, String.class);
		}
		assertThat(response.getStatusCode().is2xxSuccessful())
				.as("Workbench repositories page should be available")
				.isTrue();
		assertThat(response.getBody())
				.as("Workbench response should reference the repositories XSL transformation")
				.contains("repositories.xsl");

		ResponseEntity<String> xsl = restTemplate.getForEntity(
				"http://localhost:" + port + "/rdf4j-workbench/transformations/repositories.xsl", String.class);
		assertThat(xsl.getStatusCode().is2xxSuccessful())
				.as("Workbench should serve the repositories XSL transformation")
				.isTrue();
		assertThat(xsl.getBody())
				.as("Repositories transformation should be an XSL stylesheet")
				.contains("<xsl:stylesheet");
	}
}
