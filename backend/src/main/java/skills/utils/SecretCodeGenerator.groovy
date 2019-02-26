package skills.utils

import org.apache.commons.text.RandomStringGenerator

class SecretCodeGenerator {

    String generateSecretCode() {
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