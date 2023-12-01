package com.cubeStatBuilder.utils;

import com.cubeStatBuilder.service.GoogleSheetsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
public class CubeConditionalFormatAdder
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
			// git list of sheet theme names
			String[] themeArray = themeArrayString.split(",");

			// for each theme, delete the conditional format on the theme's sheet.
			// only use this if needed as it's extremely finicky
			for (String theme : themeArray)
			{
				if (googleSheetsService.deleteConditionalFormatForSpreadsheet(spreadsheetId, theme))
					System.out.println("Conditional formatting was removed to the " + theme + " spreadsheet!");
				else
					System.out.println("Something went wrong and the conditionally formatting for the " +  theme + " spreadsheet was NOT removed!");
			}

			// for each theme, add the conditional format on the theme's sheet.
			for (String theme : themeArray)
			{
				if (googleSheetsService.addConditionalFormatForSpreadsheet(spreadsheetId, theme))
					System.out.println("Conditional formatting was added to the " + theme + " spreadsheet!");
				else
					System.out.println("Something went wrong and the conditionally formatting for the " +  theme + " spreadsheet was NOT added!");
			}
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
