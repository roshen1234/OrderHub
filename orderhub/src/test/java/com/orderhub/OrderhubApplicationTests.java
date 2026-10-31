package com.orderhub;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mysql.MySQLContainer;

@SpringBootTest
@ActiveProfiles("test")
//@TestPropertySource("/application-test.properties")
@Testcontainers
class OrderhubApplicationTests {

	@Container
	@ServiceConnection
	static MySQLContainer mysql = new MySQLContainer("mysql:8.4")
			.withDatabaseName("orderhub_test");

	@Test
	void applicationStartsAndMigrationsRun() {
		// nothing to assert: if the context fails to start, the test fails
	}

}
