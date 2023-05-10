package se.terrassorkestern.notgen.configuration;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "se.terrassorkestern.notgen")
public class CustomProperties {

    private OcrProperties ocr;
    private StorageProperties storage;

    /**
     * The url to connect to.
     */
    @Data
    public static class OcrProperties {
        /**
         * True to enable OCR song text recognition
         */
        private boolean enable;
        /**
         * Comma separated list of UUID for the song instruments. These are the ones that will be run through the
         * OCR process.
         */
        private String songIds;
        /**
         * Username for the OCR service
         */
        private String userName = "";
        /**
         * Password for the OCR service
         */
        private String license = "";
    }

    @Data
    public static class StorageProperties {
        /**
         * Type of storage to use. The valid options are:
         * <ul>
         *   <li>local</li>
         *   <li>azure</li>
         *   <li>aws</li>
         * </ul>
         */
        private String type;
        /**
         * Location of the input files (pdf and zip archive of songs). The location is dependent of the type.
         */
        private String input;
        private String output;
        /**
         * Location of static content such as thumbnail images. This is local directory on the running host.
         */
        private String content;
        private String temp;
        private boolean keeptemp;
    }

}
