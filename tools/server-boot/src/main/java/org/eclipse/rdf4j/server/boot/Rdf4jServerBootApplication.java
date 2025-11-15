package org.eclipse.rdf4j.server.boot;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class Rdf4jServerBootApplication {

	public static void main(String[] args) {
		SpringApplication.run(Rdf4jServerBootApplication.class, args);
	}
}
