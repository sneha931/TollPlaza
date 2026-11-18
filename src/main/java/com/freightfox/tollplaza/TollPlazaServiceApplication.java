package com.freightfox.tollplaza;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
public class TollPlazaServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(TollPlazaServiceApplication.class, args);
	}

}
