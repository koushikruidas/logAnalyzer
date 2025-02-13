package com.poinciana.loganalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling

public class LoganalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(LoganalyzerApplication.class, args);
	}

}
