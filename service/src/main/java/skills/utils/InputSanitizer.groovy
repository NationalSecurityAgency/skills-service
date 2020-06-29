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

import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.safety.Whitelist

class InputSanitizer {

    public static final Document.OutputSettings print = new Document.OutputSettings().prettyPrint(false)

    static String sanitize(String input) {
        if (!input) {
            return input;
        }

        return Jsoup.clean(input, "", Whitelist.basic(), print)
    }

    /**
     * is #sanitize method is used for markdown before returning the payload back
     * to the client remove sanitizion that breaks proper display of markdown
     */
    static String unsanitizeForMarkdown(String input) {
        if (!input) {
            return input;
        }

        return input.replaceAll("&gt;", ">")
    }
}
