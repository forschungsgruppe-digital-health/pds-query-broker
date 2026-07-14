package de.tudresden.fgdh.querybroker.pdsexample;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/** Spring Boot entry point for the synthetic reference primary-data-source connector. */
@SpringBootApplication
@ConfigurationPropertiesScan
public class PrimaryDataSourceExampleApplication {

  public static void main(String[] args) {
    SpringApplication.run(PrimaryDataSourceExampleApplication.class, args);
  }
}
