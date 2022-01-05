package com.cubeStatBuilder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;

import javax.inject.Inject;

@SpringBootApplication
public class Application implements CommandLineRunner
{
	@Inject
	CubeStatBuilder cubeStatBuilder;

	public static void main(String[] args)
	{
		SpringApplication.run(Application.class, args);
	}

	@Override
	public void run(String... args) throws Exception
	{
		cubeStatBuilder.start();
	}
}
