package se.terrassorkestern.notgen2;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Path;
import java.security.GeneralSecurityException;
import java.util.Collections;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class GoogleDrive extends Google {
  private Drive service;

  public GoogleDrive() throws IOException, GeneralSecurityException {
    log.debug("Creating GoogleDrive object");

    final NetHttpTransport netHttpTransport = GoogleNetHttpTransport.newTrustedTransport();
    service = new Drive.Builder(netHttpTransport, JSON_FACTORY, getCredentials(netHttpTransport))
        .setApplicationName(APPLICATION_NAME)
        .build();
  }


  /**
   * Ladda upp fil till Google Drive
   * 
   * @param folderId        ID på mappen som man skall ladda upp til
   * @param fileType        'application/pdf' eller 'image/jpeg'
   * @param fileName        "Apertif" eller "Aperitif - sång.pdf". Extension behöver inte vara med
   * @param uploadFile      Själva filen som skall laddas upp
   * @param instrument      Om det är ett instrument så skall det ner ytterligare en katalog
   * @param description     Beskrvining av låten dvs, namn, kompoistör sättning etc.
   * @param ocr             Om filen skall OCR:as och laddas upp som ett google docs istället
   * @param map
   * @return
   * @throws IOException
   */
  public String uploadFile(
      String folderId,
      String fileType,
      String fileName,
      Path uploadFile, 
      String instrument, 
      String description, 
      Boolean ocr, 
      Map<String, String> map)
          throws IOException {

    // Måste först hitta eventuell gammal fil med samma namn
    // fileType = 'application/pdf' or 'image/jpeg'
    // Verkar inte som man kan se värdet på map i webgränssnittet. Skickar med i alla fall.
    //String baseName = Files.getNameWithoutExtension(uploadFile.getFileName().toString());

    if (instrument != null) {
      log.debug("Checking for instrument '" + instrument + "' directory");
      FileList result = service.files().list()
          .setQ("'" + folderId + "' in parents and name = '" + instrument + "' and trashed=false")
          .setPageSize(10)
          .setFields("nextPageToken, files(id, name)")
          .execute();

      if (result.getFiles().isEmpty()) {
        log.debug("Creating directory " + instrument);
        File fileMetadata = new File();
        fileMetadata.setName(instrument);
        fileMetadata.setParents(Collections.singletonList(folderId));
        fileMetadata.setMimeType("application/vnd.google-apps.folder");

        File file = service.files().create(fileMetadata)
            .setFields("id, parents")
            .execute();

        // Sätt folderID till den nyskapade katalogen
        folderId = file.getId();
        log.debug("New directory Id: " + folderId);
      } else {
        // Sätt folderID till den hittade instrumentkatalogen
        for (File f : result.getFiles()) {
          folderId = f.getId();
          log.debug("Found directory Id: " + folderId);
        }
      }
    }


    // Find any old files with the same name
    log.debug("Searching for file " + fileName);

    FileList result = service.files().list()
        .setQ("'" + folderId + "' in parents and name = '" + fileName.replaceAll("'", "\\\\'") + "' and trashed=false")
        .setSpaces("drive")
        .setFields("files(id, name)")
        .execute();

    // Delete any found files
    for (File f : result.getFiles()) {
      log.debug("Deleting file " + f.getName());
      try {
        // Av någon anledning så är metoden trash försvunnen...
        service.files().delete(f.getId()).execute();
      } catch (IOException e) {
        System.out.println("An error occurred: " + e);
      }
    }

    // Upload the new file
    log.debug("Uploading " + fileName + " to folder Id " + folderId);
    File fileMetadata = new File();
    fileMetadata.setName(fileName);
    fileMetadata.setDescription(description);
    fileMetadata.setParents(Collections.singletonList(folderId));
    fileMetadata.setProperties(map);
    if (ocr) {
      fileMetadata.setMimeType("application/vnd.google-apps.document");
    }
    java.io.File filePath = new java.io.File(uploadFile.toString());
    FileContent mediaContent = new FileContent(fileType, filePath);
    File file = service.files().create(fileMetadata, mediaContent)
        .setFields("id, parents, properties")
        .execute();
    log.debug("File ID: " + file.getId());
    
    return file.getId();
  }

  
  public void downloadFile(String folderId, String fileName, Path downloadDir) throws IOException {
    log.debug("Searching for file " + fileName);

    FileList result = this.service.files().list()
        .setQ("'" + folderId + "' in parents and name = '" + fileName.replaceAll("'", "\\\\'") + "'")
        .setFields("files(id, name)")
        .execute();

    if (result.getFiles().size() == 0) {
      log.warn("no file found for " + fileName);
    } else if (result.getFiles().size() > 1) {
      log.warn("multiple files found for " + fileName);
    } else {
      for (File f : result.getFiles()) {
        log.debug("Downloading from Google Drive, file " + f.getName());
        OutputStream outputStream = new FileOutputStream(new java.io.File(downloadDir.toFile(), fileName));
        service.files().get(f.getId())
        .executeMediaAndDownloadTo(outputStream);
      }

    }

  }
}
