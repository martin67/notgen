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
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

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
    public Path downloadScore(Score score, Path location) throws IOException {
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
    public Path downloadScorePart(ScorePart scorePart, Path location) throws IOException {
        return downloadScorePart(getScorePartName(scorePart), location);
    }

    @Override
    public Path downloadScorePart(Score score, Instrument instrument, Path location) throws IOException {
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
                HeadObjectRequest objectRequest = HeadObjectRequest.builder().bucket(outputBucket).key(getScorePartName(scorePart)).build();
                s3Client.headObject(objectRequest);
            } catch (S3Exception e) {
                log.debug("Could not find scorePart {}, {}", getScorePartName(scorePart), e.awsErrorDetails().errorMessage());
                return false;
            }
        }
        return true;
    }

    @Override
    public void uploadScore(Score score, Path path) throws IOException {

    }

    @Override
    public void uploadScorePart(ScorePart scorePart, Path path) throws IOException {
        PutObjectRequest objectRequest = PutObjectRequest.builder().bucket(outputBucket).key(getScorePartName(scorePart)).build();
        try {
            s3Client.putObject(objectRequest, RequestBody.fromFile(path));
        } catch (S3Exception e) {
            log.error("Error uploading {} to {}", path, getScorePartName(scorePart), e);
            throw new IOException(e);
        }
    }

    @Override
    public OutputStream getCoverOutputStream(Score score) throws IOException {
        return null;
    }

    @Override
    public OutputStream getThumbnailOutputStream(Score score) throws IOException {
        return null;
    }

    @Override
    public void deleteScore(Score score) throws IOException {

    }

    @Override
    public void deleteScoreParts(Score score) throws IOException {
        // List all objects in the bucket
        Set<ObjectIdentifier> objectsToDelete = new HashSet<>();
        String scoreName = String.format("%d-", score.getId());

        try {
            ListObjectsRequest listObjects = ListObjectsRequest
                    .builder()
                    .bucket(outputBucket)
                    .build();

            ListObjectsResponse res = s3Client.listObjects(listObjects);
            List<S3Object> objects = res.contents();

            for (S3Object myValue : objects) {
                if (myValue.key().startsWith(scoreName)) {
                    log.info("Deleting {}", myValue.key());
                    objectsToDelete.add(ObjectIdentifier.builder().key(myValue.key()).build());
                }
            }

            if (!objectsToDelete.isEmpty()) {
                // Delete multiple objects in one request.
                Delete del = Delete.builder()
                        .objects(objectsToDelete)
                        .build();

                DeleteObjectsRequest multiObjectDeleteRequest = DeleteObjectsRequest.builder()
                        .bucket(outputBucket)
                        .delete(del)
                        .build();

                s3Client.deleteObjects(multiObjectDeleteRequest);
            }
        } catch (S3Exception e) {
            log.error("Error deleting", e);
            throw new IOException(e);
        }
    }

    @Override
    public void cleanOutput() {

    }

}
