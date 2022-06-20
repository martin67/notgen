package se.terrassorkestern.notgen2.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.UpdateValuesResponse;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

//@Service
public class GoogleSheetService extends GoogleService {
    static final Logger log = LoggerFactory.getLogger(GoogleSheetService.class);

    private final Sheets service;


    @Value("${notgen2.google.id.spreadsheet}")
    private String googleFileIdSpreadsheet;


    private GoogleSheetService() throws IOException, GeneralSecurityException {
        log.debug("Creating GoogleSheetService object");

        final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
        service = new Sheets.Builder(netHttpTransport, JSON_FACTORY, getCredentials(netHttpTransport))
                .setApplicationName(APPLICATION_NAME)
                .build();
    }


    public void addRows(String startPos, List<List<Object>> values) {
        log.info("Uploading row to spreadsheet, startpos: " + startPos);

        ValueRange body = new ValueRange().setValues(values);
        UpdateValuesResponse result;

        try {
            result = service.spreadsheets().values().update(googleFileIdSpreadsheet, startPos, body)
                    .setValueInputOption("RAW")
                    .execute();
            log.debug(result.getUpdatedCells() + " updated cells.");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
