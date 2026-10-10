package com.orderhub;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

@SpringBootApplication
public class OrderhubApplication {

	public static void main(String[] args) {

		SpringApplication.run(OrderhubApplication.class, args);
	    System.out.println("hi");
	}

}

