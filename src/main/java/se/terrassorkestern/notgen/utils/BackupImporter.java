package se.terrassorkestern.notgen.utils;

import com.google.common.io.Files;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static java.lang.System.exit;
import static se.terrassorkestern.notgen.service.AdminService.tables;

/*
 * Simple database uploader
 *
 */

@Slf4j
public class BackupImporter {

    static final String DB_URL = "jdbc:h2:file:./todb";
    static final String USER = "sa";
    static final String PASS = "sa";

    public static void main(String[] args) throws SQLException, IOException {
        if (args.length != 1) {
            log.error("wrong argument");
            exit(-1);
        }
        String filename = args[0];

        List<String> revereseTables = new ArrayList<>(tables);
        Collections.reverse(revereseTables);
        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS)) {
            StringBuilder sb = new StringBuilder();
            for (String table : revereseTables) {
                sb.append(String.format("delete from %s; ", table));
            }
            log.debug("SQL: {}", sb);
            PreparedStatement pstmt = conn.prepareStatement(sb.toString());
            pstmt.execute();

            sb = new StringBuilder();
            sb.append("alter table score alter column id restart with 1;");
            pstmt = conn.prepareStatement(sb.toString());
            pstmt.execute();

            // import
            try (ZipInputStream zipInputStream = new ZipInputStream(new BufferedInputStream(new FileInputStream(filename)))) {
                ZipEntry zipEntry;
                CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').setHeader().build();
                while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                    log.info("Opening {}", zipEntry.getName());
                    if (Files.getFileExtension(zipEntry.getName()).equals("csv")) {
                        String table = Files.getNameWithoutExtension(zipEntry.getName());
                        Reader reader = new InputStreamReader(zipInputStream);
                        CSVParser csvParser = new CSVParser(reader, format);
                        for (CSVRecord csvRecord : csvParser.getRecords()) {
                            StringBuilder keys = new StringBuilder();
                            StringBuilder values = new StringBuilder();
                            for (Map.Entry<String, String> entry : csvRecord.toMap().entrySet()) {
                                if (entry.getValue().length() > 0) {
                                    keys.append(entry.getKey()).append(", ");
                                    values.append("'").append(escapeSql(entry.getValue())).append("'").append(", ");
                                }
                            }
                            keys.delete(keys.length() - 2, keys.length());
                            values.delete(values.length() - 2, values.length());
                            String sql = String.format("INSERT INTO %s (%s) VALUES (%s)", table, keys, values);
                            log.debug("sql: {}", sql);
                            pstmt = conn.prepareStatement(sql);
                            pstmt.execute();
                        }
                    }
                }
            }
        }
    }

    private static String escapeSql(String in) {
        return in.replaceAll("'", "''");
    }
}

