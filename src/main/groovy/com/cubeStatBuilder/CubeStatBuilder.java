package com.cubeStatBuilder;

import com.cubeStatBuilder.dto.ThemeData;
import com.cubeStatBuilder.service.GoogleSheetsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CubeStatBuilder
{
	@Value("${spreadsheetId}")
	String spreadsheetId;
	@Value("${themeArray}")
	String themeArrayString;

	void start()
	{
		// initialize Google spreadsheet service
		GoogleSheetsService googleSheetsService;
		try
		{
			googleSheetsService = new GoogleSheetsService();
		}
		catch (Exception e)
		{
			System.out.println("Something went wrong initializing the Google spreadsheet service!");
			e.printStackTrace();
			return;
		}

		try
		{
			String[] themeArray = themeArrayString.split(",");

			ThemeData themeData = googleSheetsService.getThemeDataFromSheet(spreadsheetId, themeArray[0]);
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
