package com.cubeStatBuilder;

import com.cubeStatBuilder.dto.CubeStats;
import com.cubeStatBuilder.dto.ThemeData;
import com.cubeStatBuilder.service.GoogleSheetsService;
import com.cubeStatBuilder.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

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
			// git list of sheet theme names
			String[] themeArray = themeArrayString.split(",");

			// for each theme, get its data
			List<ThemeData> themeDataList = new ArrayList<>();
			for (String theme : themeArray)
			{
				ThemeData themeData = googleSheetsService.getThemeDataFromSheet(spreadsheetId, theme);
				themeDataList.add(themeData);
			}

			CubeStats cubeStats = Utils.getCubeStatsFromThemeData(themeDataList);

			if (googleSheetsService.sendCubeStatsToSpreadsheet(cubeStats, spreadsheetId, "Cube Stats"))
				System.out.println("Cube stats were sent to the spreadsheet!");
			else
				System.out.println("Something went wrong and cube stats were NOT sent to the spreadsheet!");
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
