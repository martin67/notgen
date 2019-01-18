package se.terrassorkestern.notgen2;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleSheet extends Google {
  private Sheets service;
  private static final String GOOGLE_SPREADSHEET_ID = "1O5FEIPY2il6hPBwJRtzgjR8L4lYAII7heymn4DnVfZ4";

  public GoogleSheet() throws IOException, GeneralSecurityException {
    log.debug("Creating GoogleSheet object");

    final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
    service = new Sheets.Builder(netHttpTransport, JSON_FACTORY, getCredentials(netHttpTransport))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }


  public void addRows(String startPos, List<List<Object>> values) {
    log.info("Uploading row to spreadsheet, startpos: " +  startPos);

    ValueRange body = new ValueRange().setValues(values);
    UpdateValuesResponse result;

    try {
      result = service.spreadsheets().values().update(GOOGLE_SPREADSHEET_ID, startPos, body)
          .setValueInputOption("RAW")
          .execute();
      log.debug(result.getUpdatedCells() + " updated cells.");
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
