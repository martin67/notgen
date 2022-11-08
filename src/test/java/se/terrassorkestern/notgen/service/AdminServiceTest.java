package se.terrassorkestern.notgen.service;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import se.terrassorkestern.notgen.repository.InstrumentRepository;
import se.terrassorkestern.notgen.repository.ScoreRepository;
import se.terrassorkestern.notgen.repository.SettingRepository;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.SQLException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

@Slf4j
@SpringBootTest
@Transactional
@Tag("manual")
class AdminServiceTest {
    @Autowired
    AdminService adminService;
    @Autowired
    ScoreRepository scoreRepository;
    @Autowired
    InstrumentRepository instrumentRepository;
    @Autowired
    SettingRepository settingRepository;

    @Test
    void exportData() throws IOException, SQLException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(1024);
        adminService.export(outputStream);

        try (ZipInputStream zipInputStream = new ZipInputStream(new ByteArrayInputStream(outputStream.toByteArray()))) {
            ZipEntry zipEntry;
            while ((zipEntry = zipInputStream.getNextEntry()) != null) {
                log.info("Namn: {}", zipEntry.getName());
            }
        }
    }
}