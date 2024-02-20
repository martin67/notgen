package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@SpringBootTest
class AdminServiceTest {
    @Autowired
    private AdminService adminService;

    @Test
    void exportData() throws IOException, SQLException {
        var outputStream = new ByteArrayOutputStream(1024);
        adminService.export(outputStream);

        try (var zipInputStream = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            ZipEntry zipEntry;
            int numberOfEntries = 0;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                log.info("Namn: {}", zipEntry.getName());
                numberOfEntries++;
            }
            assertThat(numberOfEntries).isEqualTo(AdminService.tables.size() + 1);
        }
    }
}