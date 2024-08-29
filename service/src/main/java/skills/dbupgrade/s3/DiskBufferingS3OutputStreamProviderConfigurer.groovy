/**
 * Copyright 2024 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
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
