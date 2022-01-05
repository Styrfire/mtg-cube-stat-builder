Download the OAuth 2.0 Client ID json from Google Cloud Platform, rename it to ```client_secret.json``` and put it under ```src/main/resources/```

If running for the first time, a browser will open to authenticate the OAuth 2 client and put the credentials in the ```tokens/``` folder.<br>

If the program fails with a 400, try deleting the ```tokens/``` folder and trying again.<br>

When running, add the following VM Options (make sure they are updated to the correct locations):<br>
```-DsessionStrFilePath="C:\\Users\\g_n_r\\source\\repos\\irsdk\\irsdk_lapTiming\\sessionStr.txt"```<br>
```-DliveStrFilePath="C:\\Users\\g_n_r\\source\\repos\\irsdk\\irsdk_lapTiming\\liveStr.txt"```<br>
```-DspreadsheetId="put spreadsheet id here"```
```-DstintSize="5"```