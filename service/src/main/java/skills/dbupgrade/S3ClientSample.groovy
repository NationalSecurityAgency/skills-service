package skills.dbupgrade

import io.awspring.cloud.s3.DiskBufferingS3OutputStream
import io.awspring.cloud.s3.DiskBufferingS3OutputStreamProvider
import org.apache.commons.io.IOUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.core.io.WritableResource
import org.springframework.stereotype.Component
import software.amazon.awssdk.services.s3.S3Client

import java.nio.charset.StandardCharsets

@Component
class S3ClientSample {
    private final S3Client s3Client;

    @Autowired
    ApplicationContext applicationContext



    void readFile() throws IOException {
//        ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
//                request -> request.bucket("s3://skillspkiint-configs/upgrade-in-progress/wal.txt"));
//        s3://skillspkiint-configs/skills-docs/nginx.conf
        Resource s3Resource = getS3Resource()
        InputStream inputStream = s3Resource.getInputStream();
        String text = IOUtils.toString(inputStream, StandardCharsets.UTF_8);

//        String fileContent = StreamUtils.copyToString(response, StandardCharsets.UTF_8);

        System.out.println(text);
    }

    private Resource getS3Resource() {
        return applicationContext.getResource("s3://skillspkiint-configs/upgrade-in-progress/wal.txt")
    }

    void writeFile() {
//        SpringApplication.run(...).getResource("s3://[S3_BUCKET_NAME]/[FILE_NAME]");
        Resource s3Resource = getS3Resource()
        try (OutputStream os = ((WritableResource) s3Resource).getOutputStream()) {
            os.write("content".getBytes());
        }

    }
}
