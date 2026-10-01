/**
 * Copyright 2026 SkillTree
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
package skills.services.openai

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider
import software.amazon.awssdk.http.ContentStreamProvider
import software.amazon.awssdk.http.SdkHttpMethod
import software.amazon.awssdk.http.SdkHttpRequest
import software.amazon.awssdk.http.auth.aws.signer.AwsV4HttpSigner
import software.amazon.awssdk.http.auth.spi.signer.SignedRequest

/** Signs model discovery, chat, and grading through the shared OpenAI SDK transport. */
class AwsOpenAiSigningInterceptor implements Interceptor {
    private final AwsCredentialsProvider credentialsProvider
    private final String region
    private final String service
    private final AwsV4HttpSigner signer = AwsV4HttpSigner.create()

    AwsOpenAiSigningInterceptor(AwsCredentialsProvider credentialsProvider, String region, String service) {
        this.credentialsProvider = credentialsProvider
        this.region = region
        this.service = service
    }

    @Override
    Response intercept(Interceptor.Chain chain) throws IOException {
        Request request = chain.request()
        Buffer buffer = new Buffer()
        request.body()?.writeTo(buffer)
        Map<String, List<String>> headers = request.headers().toMultimap()
        if (request.body()?.contentType()) {
            headers = new LinkedHashMap<>(headers)
            headers.put('Content-Type', [request.body().contentType().toString()])
        }
        Map<String, List<String>> signedHeaders = sign(request.url().uri(), request.method(), headers, buffer.readByteArray())
        Request.Builder signedRequest = request.newBuilder().removeHeader('Authorization')
        signedHeaders.each { String name, List<String> values ->
            signedRequest.removeHeader(name)
            values.each { String value -> signedRequest.addHeader(name, value) }
        }
        return chain.proceed(signedRequest.build())
    }

    private Map<String, List<String>> sign(URI uri, String method, Map<String, List<String>> headers, byte[] payload) {
        SdkHttpRequest.Builder awsRequest = SdkHttpRequest.builder().uri(uri).method(SdkHttpMethod.fromValue(method))
        headers.each { String name, List<String> values ->
            if (!name.equalsIgnoreCase('Authorization')) {
                awsRequest.putHeader(name, values)
            }
        }
        SignedRequest signed = signer.sign { builder ->
            builder.identity(credentialsProvider.resolveCredentials())
                    .request(awsRequest.build())
                    .payload(ContentStreamProvider.fromByteArray(payload))
                    .putProperty(AwsV4HttpSigner.SERVICE_SIGNING_NAME, service)
                    .putProperty(AwsV4HttpSigner.REGION_NAME, region)
        }
        return signed.request().headers()
    }
}
