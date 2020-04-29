package bank.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class ShopApplication {

	public static void main(String[] args) {
		// the following property definition is only necessary as this project contains two spring boot applications
		System.setProperty("graphql.tools.schemaLocationPattern", "**/shop.graphqls");
		SpringApplication.run(ShopApplication.class, args);
	}

}

