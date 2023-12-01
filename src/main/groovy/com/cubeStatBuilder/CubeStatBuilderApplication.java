package com.cubeStatBuilder;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.inject.Inject;

@SpringBootApplication
public class CubeStatBuilderApplication implements CommandLineRunner
{
	@Inject
	CubeStatBuilder cubeStatBuilder;

	public static void main(String[] args)
	{
		SpringApplication.run(CubeStatBuilderApplication.class, args);
	}

	@Override
	public void run(String... args)
	{
		cubeStatBuilder.start();
	}
}
