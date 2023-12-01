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
			// if row data size < 7
			//		there aren't cells with content up to the current pick
			//		this shouldn't happen because there is conditional formatting that will show up for all the cells
			//		but it occurred before conditional formatting was added
			// index 6 is <=1
			//		if it's empty or only has one piece of content, which it should because conditional formatting will
			//		add to the size
			// number value isn't 1 aka, not picked for cube
			else if (rowData.getValues().size() < 7 || rowData.getValues().get(6).size() <= 1 || rowData.getValues().get(6).getEffectiveValue().getNumberValue().intValue() != 1)
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

	public boolean addConditionalFormatForSpreadsheet(String spreadsheetId, String sheetName) throws IOException
	{
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
			return false;
		}

		// add new formatting rules
		java.util.List<Request> requests = new ArrayList<>();

		for (int i = 0; i < 300; i++) {
			List<GridRange> ranges = Collections.singletonList(new GridRange()
					.setSheetId(themeSheet.getProperties().getSheetId())
					.setStartColumnIndex(0)
					.setEndColumnIndex(7)
					.setStartRowIndex(i + 1)
					.setEndRowIndex(i + 2)
			);

			requests.add(buildConditionalFormatRuleRequest(ranges,
					"=AND($E" + (i + 2) + "=\"low\",$G" + (i + 2) + "=1)",
					"red"));
			requests.add(buildConditionalFormatRuleRequest(ranges,
					"=AND($E" + (i + 2) + "=\"high\",$G" + (i + 2) + "=0)",
					"red"));
			requests.add(buildConditionalFormatRuleRequest(ranges,
					"=AND($E" + (i + 2) + "=\"medium\",$G" + (i + 2) + "=0)",
					"yellow"));
			requests.add(buildConditionalFormatRuleRequest(ranges,
					"=AND($E" + (i + 2) + "=\"medium\",$G" + (i + 2) + "=1)",
					"green"));
			requests.add(buildConditionalFormatRuleRequest(ranges,
					"=AND($E" + (i + 2) + "=\"low\",$G" + (i + 2) + "=0)",
					"green"));
			requests.add(buildConditionalFormatRuleRequest(ranges,
					"=AND($E" + (i + 2) + "=\"high\",$G" + (i + 2) + "=1)",
					"green"));

		}

		BatchUpdateSpreadsheetRequest batchUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
		batchUpdateSpreadsheetRequest.setRequests(requests);

		sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchUpdateSpreadsheetRequest).execute();
		return true;
	}

	public boolean deleteConditionalFormatForSpreadsheet(String spreadsheetId, String sheetName) throws IOException
	{
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
			return false;
		}

		java.util.List<Request> deleteRequests = new ArrayList<>();

		// delete previous formatting rules (have to loop through each one on the spreadsheet :( )
		// if you try to delete more rules than exist on the spreadsheet, the whole batch will fail,
		// and the deleted rules will be reverted
		for (int i = 0; i < 1800; i++)
		{
			deleteRequests.add(buildDeleteConditionalFormatRuleRequest(themeSheet.getProperties().getSheetId()));
		}

		BatchUpdateSpreadsheetRequest batchDeleteUpdateSpreadsheetRequest = new BatchUpdateSpreadsheetRequest();
		batchDeleteUpdateSpreadsheetRequest.setRequests(deleteRequests);

		sheetsService.spreadsheets().batchUpdate(spreadsheetId, batchDeleteUpdateSpreadsheetRequest).execute();
		return true;
	}

	private ColorStyle setColorStyle(String color)
	{
		return switch (color) {
			case "green" -> new ColorStyle().setRgbColor(new com.google.api.services.sheets.v4.model.Color()
					.setRed(0.7176470588235294f)
					.setGreen(0.8823529411764706f)
					.setBlue(0.803921568627451f)
			);
			case "yellow" -> new ColorStyle().setRgbColor(new com.google.api.services.sheets.v4.model.Color()
					.setRed(0.9882352941176471f)
					.setGreen(0.9098039215686274f)
					.setBlue(0.6980392156862745f)
			);
			case "red" -> new ColorStyle().setRgbColor(new com.google.api.services.sheets.v4.model.Color()
					.setRed(0.9568627450980393f)
					.setGreen(0.7803921568627451f)
					.setBlue(0.7647058823529411f)
			);
			default -> null;
		};
	}

	private Request buildConditionalFormatRuleRequest(List<GridRange> ranges, String customFormulaValue, String colorStyle){
		return new Request().setAddConditionalFormatRule(new AddConditionalFormatRuleRequest()
				.setRule(new ConditionalFormatRule()
						.setRanges(ranges)
						.setBooleanRule(new BooleanRule()
								.setCondition(new BooleanCondition()
										.setType("CUSTOM_FORMULA")
										.setValues(Collections.singletonList(
												new ConditionValue().setUserEnteredValue(
														customFormulaValue)
										))
								)
								.setFormat(new CellFormat().setBackgroundColorStyle(setColorStyle(colorStyle)))
						)
				)
				.setIndex(0)
		);
	}

	private Request buildDeleteConditionalFormatRuleRequest(int sheetId){
		return new Request().setDeleteConditionalFormatRule(new DeleteConditionalFormatRuleRequest()
				.setSheetId(sheetId)
				.setIndex(0)
		);
	}
}
