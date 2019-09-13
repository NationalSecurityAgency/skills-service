package skills.utils

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist

class InputSanitizer {


    public static final Document.OutputSettings print = new Document.OutputSettings().prettyPrint(false)

    public static String sanitize(String input) {
        if (!input) {
            return input;
        }

        return Jsoup.clean(input, "", Whitelist.basic(), print)
    }
}
