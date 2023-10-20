package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class AdminService {

    public static final List<String> tables = List.of("band", "score", "instrument", "role", "user_",
            "setting", "playlist", "privilege", "ngfile", "link", "arrangement", "user_band", "arrangement_instrument",
            "setting_instrument", "score_playlist", "role_privilege");
    private final DataSource dataSource;

    public AdminService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void export(OutputStream outputStream) throws IOException, SQLException {
        var format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').build();

        try (ZipOutputStream zos = new ZipOutputStream(outputStream);
             var conn = dataSource.getConnection()) {

            var sb = "Database dump created: " + LocalDateTime.now() +
                    "\nDatabase type:         " + conn.getMetaData().getDatabaseProductName() +
                    "\nDatabase url:          " + conn.getMetaData().getURL();
            var infoEntry = new ZipEntry("info.txt");
            zos.putNextEntry(infoEntry);
            zos.write(sb.getBytes());
            zos.closeEntry();

            for (String table : tables) {
                var filename = table + ".csv";
                var entry = new ZipEntry(filename); // create a zip entry and add it to ZipOutputStream
                zos.putNextEntry(entry);
                var sql = "select * from " + table;
                var csvPrinter = new CSVPrinter(new OutputStreamWriter(zos), format);  // There is no need for staging the CSV on filesystem or reading bytes into memory. Directly write bytes to the output stream.
                try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                    var rs = pstmt.executeQuery();
                    csvPrinter.printRecords(rs, true);
                    csvPrinter.flush(); // flush the writer. Very important!
                }
                zos.closeEntry(); // close the entry. Note : we are not closing the zos just yet as we need to add more files to our ZIP
            }
        }
    }
}