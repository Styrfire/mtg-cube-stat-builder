package com.cubeStatBuilder.utils;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.inject.Inject;

@SpringBootApplication
public class CubeConditionalFormatAdderApplication implements CommandLineRunner
{
	@Inject
	CubeConditionalFormatAdder cubeConditionalFormatAdder;

	public static void main(String[] args)
	{
		SpringApplication.run(CubeConditionalFormatAdderApplication.class, args);
	}

	@Override
	public void run(String... args)
	{
		cubeConditionalFormatAdder.start();
	}
}
