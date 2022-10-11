package se.terrassorkestern.notgen.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import se.terrassorkestern.notgen.model.Instrument;
import se.terrassorkestern.notgen.model.Score;
import se.terrassorkestern.notgen.model.ScorePart;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.core.sync.ResponseTransformer;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.nio.file.Path;

@Slf4j
@Service
public class S3Storage implements BackendStorage {

    private final S3Client s3Client;
    @Value("${notgen.storage.s3.input-bucket}")
    private String inputBucket;
    @Value("${notgen.storage.s3.output-bucket}")
    private String outputBucket;

    public S3Storage() {
        Region region = Region.EU_NORTH_1;
        s3Client = S3Client.builder().region(region).build();
    }

    public void listBuckets() {
        // List buckets
        ListBucketsRequest listBucketsRequest = ListBucketsRequest.builder().build();
        ListBucketsResponse listBucketsResponse = s3Client.listBuckets(listBucketsRequest);
        listBucketsResponse.buckets().forEach(x -> System.out.println(x.name()));
    }

    @Override
    public Path download(Score score, Path location) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(inputBucket).key(score.getFilename()).build();
        try {
            s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(location.resolve(score.getFilename())));
            return location.resolve(score.getFilename());
        } catch (S3Exception e) {
            log.error("Error downloading {} to {}", score.getFilename(), location, e);
            throw new IOException(e);
        }
    }

    @Override
    public Path download(ScorePart scorePart, Path location) throws IOException {
        return downloadScorePart(scorePart.getPdfName(), location);
    }

    @Override
    public Path download(Score score, Instrument instrument, Path location) throws IOException {
        return downloadScorePart(getScorePartName(score, instrument), location);
    }

    private Path downloadScorePart(String filename, Path location) throws IOException {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder().bucket(outputBucket).key(filename).build();
        try {
            s3Client.getObject(getObjectRequest, ResponseTransformer.toFile(location.resolve(filename)));
            return location.resolve(filename);
        } catch (S3Exception e) {
            log.error("Error downloading {} to {}", filename, location, e);
            throw new IOException(e);
        }
    }

    @Override
    public boolean isScoreGenerated(Score score) {
        for (ScorePart scorePart : score.getScoreParts()) {
            try {
                HeadObjectRequest objectRequest = HeadObjectRequest.builder().bucket(outputBucket).key(scorePart.getPdfName()).build();
                s3Client.headObject(objectRequest);
            } catch (S3Exception e) {
                log.debug("Could not find scorePart {}, {}", scorePart.getPdfName(), e.awsErrorDetails().errorMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(outputBucket).key(scorePart.getPdfName()).build();
        try {
            s3Client.putObject(objectRequest, RequestBody.fromFile(path));
        } catch (S3Exception e) {
            log.error("Error uploading {} to {}", path, scorePart.getPdfName(), e);
            throw new IOException(e);
        }
    }

    @Override
    public void cleanOutput() {

    }

    private String getScorePartName(Score score, Instrument instrument) {
        return String.format("%d-%d.pdf", score.getId(), instrument.getId());
    }

}
