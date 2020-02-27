/**
 * Copyright 2020 SkillTree
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
package skills.utils

import org.apache.commons.text.RandomStringGenerator

class ClientSecretGenerator {

    String generateClientSecret() {
        String pwString = generateRandomNumbers(12)
                .concat(generateRandomAlphabet(10, true))
                .concat(generateRandomAlphabet(12, false))
        List<Character> pwChars = pwString.toCharArray() as List
        Collections.shuffle(pwChars)
        String password = pwChars.take(32).join('')
        return password
    }

    private String generateRandomNumbers(int length) {
        return new RandomStringGenerator.Builder().withinRange(48, 57).build().generate(length)
    }

    private String generateRandomAlphabet(int length, boolean lowerCase) {
        int low
        int hi
        if (lowerCase) {
            low = 97
            hi = 122
        } else {
            low = 65
            hi = 90
        }
        return new RandomStringGenerator.Builder().withinRange(low, hi).build().generate(length)
    }
}