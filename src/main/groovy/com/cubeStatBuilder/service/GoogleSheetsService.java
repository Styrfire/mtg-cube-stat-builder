package com.cubeStatBuilder.service;

import com.cubeStatBuilder.dto.Card;
import com.cubeStatBuilder.dto.CubeStats;
import com.cubeStatBuilder.dto.ThemeData;
import com.cubeStatBuilder.utils.CardType;
import com.cubeStatBuilder.utils.Color;
import com.cubeStatBuilder.utils.Utils;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.api.services.sheets.v4.model.*;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.GeneralSecurityException;
import java.util.*;

public class GoogleSheetsService
{
	Sheets sheetsService;
	JsonFactory jsonFactory;

	public GoogleSheetsService() throws IOException, GeneralSecurityException
	{
		NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		jsonFactory = GsonFactory.getDefaultInstance();

		sheetsService = new Sheets.Builder(httpTransport, jsonFactory, getCredentials(httpTransport))
				.setApplicationName("MTG Cube Stat Builder")
				.build();
	}

	private Credential getCredentials(NetHttpTransport httpTransport) throws IOException
	{
		// global instance of the scopes required by this quickstart
		// if modifying these scopes, delete your previously saved tokens/ folder.
		List<String> scopes = Collections.singletonList(SheetsScopes.SPREADSHEETS);

		// load client secrets
		String credentialsFilePath = "/client_secret.json";
		InputStream in = GoogleSheetsService.class.getResourceAsStream(credentialsFilePath);
		if (in == null)
		{
			throw new FileNotFoundException("Resource not found: " + credentialsFilePath);
		}
		GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

		// build flow and trigger user authorization request
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, clientSecrets, scopes)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
				.setAccessType("offline")
				.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	public ThemeData getThemeDataFromSheet(String spreadsheetId, String sheetName) throws IOException, NumberFormatException
	{
		ThemeData themeData = new ThemeData();
		themeData.setName(sheetName);
		themeData.setCards(new ArrayList<>());

		// get grid data
		Spreadsheet spreadsheetWithGrid = sheetsService.spreadsheets().get(spreadsheetId)
				.setIncludeGridData(true).execute();
		List<Sheet> sheets = spreadsheetWithGrid.getSheets();

		Sheet themeSheet = findSheetByTitle(sheets, sheetName);
		if (themeSheet != null)
			System.out.println("Found existing theme sheet!");
		else
		{
			System.out.println("Didn't find an existing theme sheet!");
			return null;
		}

		List<GridData> gridData = spreadsheetWithGrid.getSheets().get(themeSheet.getProperties().getIndex()).getData();

		// for each row starting with the second
		for (int i = 1; i < gridData.get(0).getRowData().size(); i++)
		{
			RowData rowData = gridData.get(0).getRowData().get(i);

			// check if the card name is empty
			if (rowData.getValues().get(0).getEffectiveValue() == null)
			{
				System.out.println("Card not found! Must be at the end of the list!");
				break;
			}
			// if row data size < 7, index 6 is empty, or number value isn't 1 aka, not picked for cube
			else if (rowData.getValues().size() < 7 || rowData.getValues().get(6).size() == 0 || rowData.getValues().get(6).getEffectiveValue().getNumberValue().intValue() != 1)
			{
				System.out.println(rowData.getValues().get(0).getEffectiveValue().getStringValue() +
						" was not selected to be in the cube!");
			}
			else
			{
				// add card to theme
				Card card = new Card();
				card.setCardName(rowData.getValues().get(0).getEffectiveValue().getStringValue());
				card.setCmc(rowData.getValues().get(1).getEffectiveValue().getNumberValue().intValue());
				// do a bunch of color stuff...
				String colorCombo = rowData.getValues().get(2).getEffectiveValue().getStringValue();
				List <Color> colors = new ArrayList<>();
				for (char color : colorCombo.toCharArray())
				{
					switch (color) {
						case 'W' -> colors.add(Color.White);
						case 'U' -> colors.add(Color.Blue);
						case 'B' -> colors.add(Color.Black);
						case 'R' -> colors.add(Color.Red);
						case 'G' -> colors.add(Color.Green);
						case 'C' -> colors.add(Color.Colorless);
					}

				}
				card.setColors(colors);
				card.setCardType(CardType.valueOf(rowData.getValues().get(3).getEffectiveValue().getStringValue()));

				themeData.getCards().add(card);
				System.out.println("Added the following card to the " + sheetName + " theme!\n" + card);
			}
		}

		themeData.setNumOfCards(themeData.getCards().size());
		themeData.setColorTypeAmountMatrix(Utils.getColorTypeAmountMatrixFromCardList(themeData.getCards()));

		System.out.println(themeData.getName() + " had " + themeData.getNumOfCards() + " cards in it!");

		return themeData;
	}

	public boolean sendThemeStatsToSpreadsheet(int[][] colorTypeAmountMatrix, String spreadsheetId, String sheetName) throws IOException
	{
		// set cell values with matrix
		List<List<Object>> cellValues = new ArrayList<>();
		for (int i = 0; i < 7; i++)
		{
			List<Object> rowValues = new ArrayList<>();
			for (int j = 0; j < 8; j++)
				rowValues.add(colorTypeAmountMatrix[i][j]);

			cellValues.add(rowValues);
		}

		// get spreadsheet
		Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();

		// shove the data in the spreadsheet at the appropriate column
		ValueRange valueRange = new ValueRange();
		valueRange.setValues(cellValues);
		Sheets.Spreadsheets.Values.Update request = sheetsService.spreadsheets().values()
				.update(spreadsheet.getSpreadsheetId(), "'" + sheetName + "'!J2:Q8", valueRange);
		request.setValueInputOption("RAW").execute();

		return true;
	}

	public boolean sendCubeStatsToSpreadsheet(CubeStats cubeStats, String spreadsheetId, String sheetName) throws IOException
	{
		// set cell values with matrix
		List<List<Object>> cellValues = new ArrayList<>();
		for (int i = 0; i < 7; i++)
		{
			List<Object> rowValues = new ArrayList<>();
			for (int j = 0; j < 8; j++)
				rowValues.add(cubeStats.getColorTypeAmountMatrix()[i][j]);

			cellValues.add(rowValues);
		}

		// get spreadsheet
		Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();

		// shove the data in the spreadsheet at the appropriate column
		ValueRange valueRange = new ValueRange();
		valueRange.setValues(cellValues);
		Sheets.Spreadsheets.Values.Update request = sheetsService.spreadsheets().values()
				.update(spreadsheet.getSpreadsheetId(), "'" + sheetName + "'!B6:I12", valueRange);
		request.setValueInputOption("RAW").execute();

		return true;
	}

	private Sheet findSheetByTitle(List<Sheet> sheets, String sheetTitle)
	{
		for (Sheet sheet : sheets)
			if (Objects.equals(sheet.getProperties().getTitle(), sheetTitle))
				return sheet;

		return null;
	}
}
