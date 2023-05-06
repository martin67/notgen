package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.json.JsonParser;
import org.springframework.boot.json.JsonParserFactory;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Arrangement;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.repository.InstrumentRepository;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/*
 *
 * Sample class for OCRWebService.com (REST API)
 *
 */
@Slf4j
@Service
public class SongOcrService {

    @Value("${se.terrassorkestern.notgen.ocr.enable:false}")
    private boolean enableOcr;
    @Value("${se.terrassorkestern.notgen.ocr.songids:0}")
    private String songIds;
    @Value("${se.terrassorkestern.notgen.ocr.username:}")
    private String username;
    @Value("${se.terrassorkestern.notgen.ocr.license:}")
    private String license;

    private final InstrumentRepository instrumentRepository;
    private final StorageService storageService;

    public SongOcrService(InstrumentRepository instrumentRepository, StorageService storageService) {
        this.instrumentRepository = instrumentRepository;
        this.storageService = storageService;
    }

    public String process(Score score) throws Exception {
        /*
        	Sample project for OCRWebService.com (REST API).
        	Extract text from scanned images and convert into editable formats.
        	Please create new account with ocrwebservice.com via http://www.ocrwebservice.com/account/signup and get license code
		 */

        if (!enableOcr) {
            return "OCR disabled";
        }

         /*

	       You should specify OCR settings. See full description http://www.ocrwebservice.com/service/restguide

	       Input parameters:

		   [language]      - Specifies the recognition language.
		   		    		 This parameter can contain several language names separated with commas.
	                         For example "language=english,german,spanish".
				    		 Optional parameter. By default:english

		   [pagerange]     - Enter page numbers and/or page ranges separated by commas.
				    		 For example "pagerange=1,3,5-12" or "pagerange=allpages".
	                         Optional parameter. By default:allpages

	       [tobw]	  	   - Convert image to black and white (recommend for color image and photo).
				    		 For example "tobw=false"
	                         Optional parameter. By default:false

	       [zone]          - Specifies the region on the image for zonal OCR.
				    		 The coordinates in pixels relative to the left top corner in the following format: top:left:height:width.
				    		 This parameter can contain several zones separated with commas.
			            	 For example "zone=0:0:100:100,50:50:50:50"
	                         Optional parameter.

	       [outputformat]  - Specifies the output file format.
	                         Can be specified up to two output formats, separated with commas.
				    		 For example "outputformat=pdf,txt"
	                         Optional parameter. By default:doc

	       [gettext]	   - Specifies that extracted text will be returned.
				    		 For example "tobw=true"
	                         Optional parameter. By default:false

	        [description]  - Specifies your task description. Will be returned in response.
	                         Optional parameter.


		   !!!!  For getting result you must specify "gettext" or "outputformat" !!!!

		*/

        // Build your OCR:

        // Extraction text with English language
        String ocrURL = "https://www.ocrwebservice.com/restservices/processDocument?gettext=true&language=swedish&newline=1";

        // Full path to uploaded document
        Instrument song = instrumentRepository.getReferenceById(UUID.fromString(songIds));
        Path tempDir = storageService.createTempDir();
        Arrangement arrangement = score.getDefaultArrangement();
        Path path = storageService.downloadArrangementPart(arrangement, song, tempDir);

        byte[] fileContent = Files.readAllBytes(path);

        URL url = new URL(ocrURL);

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setDoOutput(true);
        connection.setDoInput(true);
        connection.setRequestMethod("POST");
        connection.setRequestProperty("Authorization", "Basic " + Base64.getEncoder().encodeToString((username + ":" + license).getBytes()));
        // Specify Response format to JSON or XML (application/json or application/xml)
        connection.setRequestProperty("Content-Type", "application/json");
        connection.setRequestProperty("Content-Length", Integer.toString(fileContent.length));

        OutputStream stream = connection.getOutputStream();

        // Send POST request
        stream.write(fileContent);
        stream.close();

        int httpCode = connection.getResponseCode();

        log.info("HTTP Response code: " + httpCode);

        // Success request
        if (httpCode == HttpURLConnection.HTTP_OK) {
            // Get response stream
            String jsonResponse = getResponseToString(connection.getInputStream());

            // Parse and print response from OCR server
            JsonParser parser = JsonParserFactory.getJsonParser();
            Map<String, Object> jsonObj = parser.parseMap(jsonResponse);
            List<List<String>> texts = (List<List<String>>) jsonObj.get("OCRText");
            return texts.get(0).get(0);
        } else if (httpCode == HttpURLConnection.HTTP_UNAUTHORIZED) {
            log.error("OCR Error Message: Unauthorized request");
        } else {
            // Error occurred
            String jsonResponse = getResponseToString(connection.getErrorStream());

            JsonParser parser = JsonParserFactory.getJsonParser();
            Map<String, Object> jsonObj = parser.parseMap(jsonResponse);

            // Error message
            log.error("Error Message: " + jsonObj.get("ErrorMessage"));
        }

        connection.disconnect();

        return "";
    }

    private static String getResponseToString(InputStream inputStream) throws IOException {
        InputStreamReader responseStream = new InputStreamReader(inputStream);

        BufferedReader br = new BufferedReader(responseStream);
        StringBuilder strBuff = new StringBuilder();
        String s;
        while ((s = br.readLine()) != null) {
            strBuff.append(s);
        }
        return strBuff.toString();
    }

}
