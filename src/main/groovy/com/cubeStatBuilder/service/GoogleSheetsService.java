package com.cubeStatBuilder.service;

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
		GoogleClientSecrets  clientSecrets = GoogleClientSecrets.load(jsonFactory, new InputStreamReader(in));

		// build flow and trigger user authorization request
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
				httpTransport, jsonFactory, clientSecrets, scopes)
				.setDataStoreFactory(new FileDataStoreFactory(new java.io.File("tokens")))
				.setAccessType("offline")
				.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();

		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

//	public void sendStintDataToSpreadsheet(Stint stint, String spreadsheetId) throws Exception
//	{
//		Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
//		List<Sheet> sheets = spreadsheet.getSheets();
//
//		Sheet trackSheet = findSheetByTitle(sheets, stint.getTrackName());
//		if (trackSheet != null)
//			System.out.println("Found existing track sheet!");
//		else
//		{
//			System.out.println("Didn't find an existing track sheet. Creating one now.");
//			trackSheet = createNewTrackSheet(spreadsheet, stint.getTrackName());
//		}
//
//		updateTrackSheetLapTimes(stint, spreadsheet, trackSheet);
//	}

	private Sheet createNewTrackSheet(Spreadsheet spreadsheet, String sheetTitle) throws Exception
	{
		List<Sheet> sheets = spreadsheet.getSheets();

		Sheet baseSheet = findSheetByTitle(sheets, "Base");
		if (baseSheet == null)
		{
			System.out.println("baseSheet = null!");
			throw new Exception("baseSheet = null");
		}

		System.out.println("Created new track sheet!");
		CopySheetToAnotherSpreadsheetRequest copySheetRequestBody = new CopySheetToAnotherSpreadsheetRequest();
		copySheetRequestBody.setDestinationSpreadsheetId(spreadsheet.getSpreadsheetId());
		SheetProperties sheetProperties = sheetsService.spreadsheets().sheets().copyTo(spreadsheet.getSpreadsheetId(), baseSheet.getProperties().getSheetId(), copySheetRequestBody).execute();

		// create sheet properties
		SheetProperties updateSheetProperties = new SheetProperties();
		updateSheetProperties.setSheetId(sheetProperties.getSheetId());
		updateSheetProperties.setTitle(sheetTitle);

		// create update sheet request
		UpdateSheetPropertiesRequest updateSheetRequestBody = new UpdateSheetPropertiesRequest();
		updateSheetRequestBody.setProperties(updateSheetProperties);
		updateSheetRequestBody.setFields("title"); // which field(s) get(s) updated

		Request updateSheetRequest = new Request();
		updateSheetRequest.setUpdateSheetProperties(updateSheetRequestBody);

		List<Request> requests = new ArrayList<>();
		requests.add(updateSheetRequest);

		BatchUpdateSpreadsheetRequest batchUpdateRequestBody = new BatchUpdateSpreadsheetRequest();
		batchUpdateRequestBody.setRequests(requests);
		batchUpdateRequestBody.setIncludeSpreadsheetInResponse(true);
		BatchUpdateSpreadsheetResponse response = sheetsService.spreadsheets().batchUpdate(spreadsheet.getSpreadsheetId(), batchUpdateRequestBody).execute();
		sheets = response.getUpdatedSpreadsheet().getSheets();

		Sheet newTrackSheet = findSheetByTitle(sheets, sheetTitle);
		if (newTrackSheet == null)
		{
			System.out.println("newTrackSheet = null!");
			throw new Exception("newTrackSheet = null");
		}

		return newTrackSheet;
	}

	private Sheet findSheetByTitle(List<Sheet> sheets, String sheetTitle)
	{
		for (Sheet sheet: sheets)
			if (Objects.equals(sheet.getProperties().getTitle(), sheetTitle))
				return sheet;

		return null;
	}

	// if an empty column is available, use that for the stint, else return false
//	private void updateTrackSheetLapTimes(Stint stint, Spreadsheet spreadsheet, Sheet trackSheet) throws Exception
//	{
//		boolean foundEmptyRow = false;
//
//		// get grid data
//		Spreadsheet spreadsheetWithGrid = sheetsService.spreadsheets().get(spreadsheet.getSpreadsheetId())
//				.setIncludeGridData(true).execute();
//		List<GridData> gridData = spreadsheetWithGrid.getSheets().get(trackSheet.getProperties().getIndex()).getData();
//
//		Map<Integer, String> mergedColumnLetterMap = new HashMap<>();
//		mergedColumnLetterMap.put(1, "BC");
//		mergedColumnLetterMap.put(2, "DE");
//		mergedColumnLetterMap.put(3, "FG");
//		mergedColumnLetterMap.put(4, "HI");
//		mergedColumnLetterMap.put(5, "JK");
//		mergedColumnLetterMap.put(6, "LM");
//		mergedColumnLetterMap.put(7, "NO");
//		mergedColumnLetterMap.put(8, "PQ");
//
//		// get row B2:C2:P2:Q2
//		RowData rowB2C2ToP2Q2 = gridData.get(0).getRowData().get(1);
//		for (int i = 1; i < rowB2C2ToP2Q2.getValues().size(); i++)
//		{
//			// if this column is free, update its data!
//			if (rowB2C2ToP2Q2.getValues().get(i).getEffectiveValue() == null)
//			{
//				// get current column letters for merged cell
//				String mergeColumn1 = String.valueOf(mergedColumnLetterMap.get(i).charAt(0));
//				String mergeColumn2 = String.valueOf(mergedColumnLetterMap.get(i).charAt(1));
//
//				// set cell values with setup name and lap times, and fill the rest (up to 21) with empty
//				List<Object> cellValuesLapTimes = new ArrayList<>();
//				cellValuesLapTimes.add(stint.getSetupName());
//				cellValuesLapTimes.addAll(stint.getStintLapTimes());
//				for (int j = cellValuesLapTimes.size(); j < 21; j++)
//					cellValuesLapTimes.add("");
//
//				// add cell values to 2d value array
//				List<List<Object>> valuesLapTimes = new ArrayList<>();
//				for (Object cellValue : cellValuesLapTimes)
//				{
//					List<Object> item = new ArrayList<>();
//					item.add(cellValue);
//					valuesLapTimes.add(item);
//				}
//
//				// shove the data in the spreadsheet at the appropriate column
//				ValueRange valueRangeLapTimes = new ValueRange();
//				valueRangeLapTimes.setValues(valuesLapTimes);
//				Sheets.Spreadsheets.Values.Update requestLapTimes = sheetsService.spreadsheets().values()
//						.update(spreadsheet.getSpreadsheetId(),
//								"'" + trackSheet.getProperties().getTitle() + "'!" + mergeColumn1 + "2:" + mergeColumn2 + "22", valueRangeLapTimes);
//				requestLapTimes.setValueInputOption("RAW").execute();
//
//				// set cell values with tire temps and treads
//				List<List<Object>> cellValuesTires = new ArrayList<>();
//				List<Object> leftRightTires = new ArrayList<>();
//				leftRightTires.add(stint.getTires().getLeftFront().getLastTempsOMI());
//				leftRightTires.add(stint.getTires().getRightFront().getLastTempsOMI());
//				cellValuesTires.add(leftRightTires);
//				leftRightTires = new ArrayList<>();
//				leftRightTires.add(stint.getTires().getLeftRear().getLastTempsOMI());
//				leftRightTires.add(stint.getTires().getRightRear().getLastTempsOMI());
//				cellValuesTires.add(leftRightTires);
//				leftRightTires = new ArrayList<>();
//				leftRightTires.add(stint.getTires().getLeftFront().getTreadRemaining());
//				leftRightTires.add(stint.getTires().getRightFront().getTreadRemaining());
//				cellValuesTires.add(leftRightTires);
//				leftRightTires = new ArrayList<>();
//				leftRightTires.add(stint.getTires().getLeftRear().getTreadRemaining());
//				leftRightTires.add(stint.getTires().getRightRear().getTreadRemaining());
//				cellValuesTires.add(leftRightTires);
//
//				// add cell values to 2d value array
//				List<List<Object>> valuesTires = new ArrayList<>();
//				for (List<Object> cellValue : cellValuesTires)
//				{
//					List<Object> item = new ArrayList<>();
//					item.add(cellValue.get(0)); // left tire
//					item.add(cellValue.get(1)); // right tire
//					valuesTires.add(item);
//				}
//
//				// shove the data in the spreadsheet at the appropriate column
//				ValueRange valueRangeTires = new ValueRange();
//				valueRangeTires.setValues(valuesTires);
//				Sheets.Spreadsheets.Values.Update requestTires = sheetsService.spreadsheets().values()
//						.update(spreadsheet.getSpreadsheetId(),
//								"'" + trackSheet.getProperties().getTitle() + "'!" + mergeColumn1 + "25:" + mergeColumn2 + "28", valueRangeTires);
//				requestTires.setValueInputOption("RAW").execute();
//
//				System.out.println("\nAdded sprint on column " + mergeColumn1 + " in the " + stint.getTrackName() + " sheet!\n");
//
//				foundEmptyRow = true;
//				break;
//			}
//		}
//
//		if (!foundEmptyRow)
//		{
//			System.out.println("There was not a free row to put the data! Make more space on the spreadsheet!");
//			throw new Exception("There was not a free row to put the data! Make more space on the spreadsheet!");
//		}
//	}
}
