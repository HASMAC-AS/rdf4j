package org.eclipse.rdf4j.serverboot;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Import;

@SpringBootApplication
@Import({ ServerWebConfiguration.class, WorkbenchWebConfiguration.class })
public class Rdf4jEmbeddedApplication extends SpringBootServletInitializer {

	public static void main(String[] args) {
		new SpringApplicationBuilder(Rdf4jEmbeddedApplication.class)
				.properties("spring.main.allow-bean-definition-overriding=true")
				.build()
				.run(args);
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return builder.properties("spring.main.allow-bean-definition-overriding=true");
	}
}
