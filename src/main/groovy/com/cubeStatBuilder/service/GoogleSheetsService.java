package com.cubeStatBuilder.service;

import com.cubeStatBuilder.dto.Card;
import com.cubeStatBuilder.dto.ThemeData;
import com.cubeStatBuilder.utils.CardType;
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
import java.text.ParseException;
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
				.setApplicationName("iRacing Stint Analyzer")
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

		for (int i = 1; i < gridData.get(0).getRowData().size(); i++)
		{
			RowData rowData = gridData.get(0).getRowData().get(i);

			if (rowData.getValues().get(0).getEffectiveValue() == null)
			{
				System.out.println("Card not found! Must be at the end of the list!");
				break;
			}
			else
			{
				Card card = new Card();
				card.setCardName(rowData.getValues().get(0).getEffectiveValue().getStringValue());
				card.setCmc(rowData.getValues().get(1).getEffectiveValue().getNumberValue().intValue());
				//card.setColor(rowData.getValues().get(1).getEffectiveValue().getStringValue());
				card.setCardType(CardType.valueOf(rowData.getValues().get(3).getEffectiveValue().getStringValue()));

				themeData.getCards().add(card);
				System.out.println("Added the following card to the " + sheetName + " theme!\n" + card);
			}
		}

		return null;
	}

	private Sheet findSheetByTitle(List<Sheet> sheets, String sheetTitle)
	{
		for (Sheet sheet : sheets)
			if (Objects.equals(sheet.getProperties().getTitle(), sheetTitle))
				return sheet;

		return null;
	}
}
