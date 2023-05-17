package se.terrassorkestern.notgen.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.cli.*;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import static se.terrassorkestern.notgen.service.AdminService.tables;

/*
 * Simple database uploader
 * on Windows, run with -Dfile.encoding=UTF8
 * -d jdbc:h2:file:./todb
 */

@Slf4j
public class BackupImporter {

    public static void main(String[] args) throws SQLException, IOException {

        CommandLineParser parser = new DefaultParser();
        Options options = new Options();
        options.addOption("d", "database", true, "Database URI");
        options.addOption("u", "username", true, "Username");
        options.addOption("p", "password", true, "Password");

        String database;
        String username;
        String password;
        String filename;
        try {
            CommandLine cmd = parser.parse(options, args);
            database = cmd.getOptionValue("database");
            username = cmd.getOptionValue("username");
            password = cmd.getOptionValue("password");

            if (cmd.getArgs().length != 1) {
                throw new IllegalArgumentException("Need filename as only argument");
            } else {
                filename = cmd.getArgs()[0];
            }
        } catch (ParseException e) {
            throw new RuntimeException(e);
        }

        List<String> reverseTables = new ArrayList<>(tables);
        Collections.reverse(reverseTables);
        try (Connection conn = DriverManager.getConnection(database, username, password)) {
            StringBuilder sb = new StringBuilder();
            sb.append("alter table score drop constraint score_arrangement_id_fk; ");
            for (String table : reverseTables) {
                sb.append(String.format("delete from %s; ", table));
            }
            log.trace("SQL: {}", sb);
            try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
                pstmt.execute();
            }

            // import

            try (ZipFile zipFile = new ZipFile(filename)) {
                CSVFormat format = CSVFormat.Builder.create(CSVFormat.EXCEL).setDelimiter(';').setHeader().build();
                for (String table : tables) {
                    log.debug("Reading table: {}", table);
                    ZipEntry zipEntry = zipFile.getEntry(table + ".csv");
                    Reader reader = new InputStreamReader(zipFile.getInputStream(zipEntry), StandardCharsets.UTF_8);
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
                        log.trace("sql: {}", sql);
                        try (PreparedStatement pstmt = conn.prepareStatement(sql)) {
                            pstmt.execute();
                        }
                    }
                }

            }
            sb = new StringBuilder();
            sb.append("alter table score add constraint score_arrangement_id_fk" +
                    "        foreign key (default_arrangement_id) references arrangement;");
            try (PreparedStatement pstmt = conn.prepareStatement(sb.toString())) {
                pstmt.execute();
            }
        }
    }

    private static String escapeSql(String in) {
        return in.replace("'", "''");
    }
}

