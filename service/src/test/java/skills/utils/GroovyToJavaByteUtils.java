/**
 * Copyright 2021 SkillTree
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
package skills.utils;

import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;

import java.util.Random;

public class GroovyToJavaByteUtils {
    public static byte[] toBytes(String s) {
        return s.getBytes();
    }

    public static ByteArrayResource toByteArrayResource(String s) {
        return new ByteArrayResource(s.getBytes());
    }

    public static ByteArrayResource toByteArrayResource(String s, String filename) {
        return new ByteArrayResource(s.getBytes()) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }

    public static ByteArrayResource toByteArrayResource(Integer fileSize, String filename) {
        byte[] bytes = new byte[fileSize];
        new Random().nextBytes(bytes);
        return new ByteArrayResource(bytes) {
            @Override
            public String getFilename() {
                return filename;
            }
        };
    }
}
