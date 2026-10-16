package com.irctc.railconnect;

import org.springframework.boot.SpringApplication;

public class TestRailconnectBackendApplication {

	public static void main(String[] args) {
		SpringApplication.from(RailconnectBackendApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
