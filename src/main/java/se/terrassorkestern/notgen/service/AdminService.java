package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
public class AdminService {

    private final DataSource dataSource;
    private final List<String> tables = List.of("score", "instrument", "organization", "playlist",
            "privilege", "role", "role_privilege", "score_instrument", "score_playlist", "setting",
            "setting_instrument", "song", "user_", "user_role");

    public AdminService(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void export(OutputStream outputStream) throws IOException, SQLException {
        CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').build();

        try (ZipOutputStream zos = new ZipOutputStream(outputStream);
             Connection conn = dataSource.getConnection()
        ) {

            String sb = "Database dump created: " + LocalDateTime.now() +
                    "\nDatabase type:         " + dataSource.getConnection().getMetaData().getDatabaseProductName() +
                    "\nDatabase url:          " + dataSource.getConnection().getMetaData().getURL();
            ZipEntry infoEntry = new ZipEntry("info.txt");
            zos.putNextEntry(infoEntry);
            zos.write(sb.getBytes());
            zos.closeEntry();

            for (String table : tables) {
                String filename = table + ".csv";
                ZipEntry entry = new ZipEntry(filename); // create a zip entry and add it to ZipOutputStream
                zos.putNextEntry(entry);

                CSVPrinter csvPrinter = new CSVPrinter(new OutputStreamWriter(zos), format);  // There is no need for staging the CSV on filesystem or reading bytes into memory. Directly write bytes to the output stream.
                PreparedStatement pstmt = conn.prepareStatement("select * from " + table);
                ResultSet rs = pstmt.executeQuery();
                csvPrinter.printRecords(rs, true);
                csvPrinter.flush(); // flush the writer. Very important!
                zos.closeEntry(); // close the entry. Note : we are not closing the zos just yet as we need to add more files to our ZIP
            }
        }
    }
}