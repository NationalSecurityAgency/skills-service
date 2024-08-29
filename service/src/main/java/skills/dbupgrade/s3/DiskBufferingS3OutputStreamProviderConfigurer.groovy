package skills.dbupgrade.s3

import groovy.util.logging.Slf4j
import io.awspring.cloud.s3.DiskBufferingS3OutputStreamProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Conditional
import org.springframework.stereotype.Service
import software.amazon.awssdk.services.s3.S3Client

@Service
@Slf4j
@Conditional(UpgradeInProgressWithS3Condition)
class DiskBufferingS3OutputStreamProviderConfigurer {

    S3Client s3Client;

    DiskBufferingS3OutputStreamProviderConfigurer(S3Client s3Client) {
        println "Using DiskBufferingS3OutputStreamProviderConfigurer with S3Client: ${s3Client}"
        this.s3Client = s3Client;
    }

    /**
     *  from awspring docs: you can use io.awspring.cloud.s3.DiskBufferingS3OutputStream by defining
     *  a bean of type DiskBufferingS3OutputStreamProvider which will override the default output stream provider.
     *  With DiskBufferingS3OutputStream when data is written to the resource,
     *  first it is stored on the disk in a tmp directory in the OS.
     * @return
     */
    @Bean
    @Conditional(UpgradeInProgressWithS3Condition)
    DiskBufferingS3OutputStreamProvider getDiskBufferingS3OutputStreamProvider() {
        return new DiskBufferingS3OutputStreamProvider((S3Client)s3Client, null)
    }
}
