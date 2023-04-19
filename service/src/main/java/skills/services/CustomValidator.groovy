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
package skills.services

import callStack.profiler.Profile
import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.QuizDefRequest
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SubjectRequest
import skills.utils.InputSanitizer

import jakarta.annotation.PostConstruct
import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Service
class CustomValidator {

    @Value('#{"${skills.config.ui.paragraphValidationRegex}"}')
    String paragraphValidationRegex

    @Value('#{"${skills.config.ui.paragraphValidationMessage}"}')
    String paragraphValidationMessage

    @Value('#{"${skills.config.ui.nameValidationRegex}"}')
    String nameValidationRegex

    @Value('#{"${skills.config.ui.nameValidationMessage}"}')
    String nameValidationMessage

    private String paragraphValidationMsg
    private Pattern paragraphPattern

    private String nameValidationMsg
    private Pattern nameRegex

    private static final Pattern BULLET = ~/^\s*(?:\d\. |\* |- )/
    private static final Pattern NEWLINE = ~/\n/
    private static final Pattern HEADER_OR_BLOCK_QUOTE = ~/^([\n]?[#>]{1,}[\s])+/
    private static final Pattern BOLD_AND_ITALICS = ~/^(\s*)[*_]{1,3}([^*_]+)[*_]{1,3}/
    private static final Pattern HTML = ~/(<[\/]?em>|<[\/]?del>|<[\/]?strong>|<[\/]?span(?: style=".+?")?>)/

    private static final Pattern TABLE_FIX = ~/(?m)(^\n)(^[|].+[|]$\n^[|].*[-]{3,}.*[|]$)/
    private static final Pattern CODEBLOCK_FIX = ~/(?m)(^\n)(^[`]{3}$)/
    private static final Pattern LIST_FIX = ~/(?m)(^\n)(\s*\d\. |\* |- .*$)/

    @PostConstruct
    CustomValidator init() {
        paragraphValidationMsg = paragraphValidationMessage ?: "Description failed validation"
        if ( StringUtils.isNotBlank(paragraphValidationRegex)){
            log.info("Configuring paragraph validator. regex=[{}], message=[{}]", paragraphValidationRegex, paragraphValidationMsg)
            paragraphPattern = Pattern.compile(paragraphValidationRegex)
        }

        nameValidationMsg = nameValidationMessage ?: "Name failed validation"
        if ( StringUtils.isNotBlank(nameValidationRegex)) {
            log.info("Configuring name validator. regex=[{}], message=[{}]", nameValidationRegex, nameValidationMsg)
            nameRegex = Pattern.compile(nameValidationRegex)
        }

        return this
    }

    CustomValidationResult validate(ProjectRequest projectRequest) {
        return validateDescriptionAndName(projectRequest.description, projectRequest.name)
    }

    CustomValidationResult validate(SubjectRequest subjectRequest) {
        return validateDescriptionAndName(subjectRequest.description, subjectRequest.name)
    }

    CustomValidationResult validate(QuizDefRequest quizDefRequest) {
        return validateDescriptionAndName(quizDefRequest.description, quizDefRequest.name)
    }

    @Profile
    CustomValidationResult validate(SkillRequest skillRequest) {
        return validateDescriptionAndName(skillRequest.description, skillRequest.name)
    }

    CustomValidationResult validate(BadgeRequest badgeRequest) {
        return validateDescriptionAndName(badgeRequest.description, badgeRequest.name)
    }

    private CustomValidationResult validateDescriptionAndName(String description, String name) {
        CustomValidationResult validationResult = validateDescription(description)
        if (!validationResult.valid) {
            return validationResult
        }

        validationResult = validateName(name)
        return validationResult
    }

    CustomValidationResult validateDescription(String description) {
        if (!paragraphPattern || StringUtils.isBlank(description)) {
            return new CustomValidationResult(valid: true)
        }
        log.debug("Validating description:\n[${description}]")

        description = InputSanitizer.unsanitizeForMarkdown(description)

        // split if
        // - there is at least 2 new lines
        // - markdown separator (3 underscores, 3 dashes, 3 stars)
        description = preProcessForMarkdownSupport(description)
        if (StringUtils.isBlank(description)) {
            return new CustomValidationResult(valid: true)
        }
        String[] paragraphs = description.split("([\n]{2,})|(\n[\\s]*[-_*]{3,})")

        CustomValidationResult validationResult = null
        for (String s : paragraphs) {
            if (!s){
                continue
            }

            String toValidate = adjustForMarkdownSupport(s)
            validationResult = validateInternal(paragraphPattern, toValidate, paragraphValidationMsg)
            if (!validationResult.valid) {
                break
            }
        }

        return validationResult
    }

    private String preProcessForMarkdownSupport(String toValidate) {
//        String toValidate = s.trim()

        // treat all linebreaks and separators as newlines
        toValidate = toValidate.replaceAll(/(?m)(^\s*[-_*]{3,}\s*$)|(^\s*<br>\s*$)/, '\n')

        // remove a single new line above a table and/or codeblock
        toValidate = TABLE_FIX.matcher(toValidate).replaceAll('$2')
        toValidate = CODEBLOCK_FIX.matcher(toValidate).replaceAll('$2')
        toValidate = LIST_FIX.matcher(toValidate).replaceAll('$2')

        return toValidate.trim()
    }

    private String adjustForMarkdownSupport(String s) {

        String toValidate = s.trim()

        // remove extra html markdown sometimes added by wysiwyg editor
        toValidate = HTML.matcher(toValidate).replaceAll("")

        // remove markdown bullets that start at the beginning of a line/paragraph
        toValidate = BULLET.matcher(toValidate).replaceAll("")

        // remove a single newline so the provided regex does not need check for newlines themselves
        // since regex . doesn't match \n
        // this important since description allows the use of markdown, for example is an input for the markdown list:
        // "some paragraph\n*item1 *item2
        toValidate = NEWLINE.matcher(toValidate).replaceAll("")

        // support markdown headers and blockquotes
        // # Header
        // ## Header
        // ### Header
        // > quote
        toValidate = HEADER_OR_BLOCK_QUOTE.matcher(toValidate).replaceAll("")

        // remove all bold and/or italics from the beginning of a line
        toValidate = BOLD_AND_ITALICS.matcher(toValidate).replaceAll('$1$2')


        return toValidate.trim()
    }

    CustomValidationResult validateName(String name) {
        if (!nameRegex || StringUtils.isBlank(name)) {
            return new CustomValidationResult(valid: true)
        }

        CustomValidationResult validationResult = validateInternal(nameRegex, name, nameValidationMsg)
        return validationResult
    }

    private CustomValidationResult validateInternal(Pattern regex, String value, String msg) {
        CustomValidationResult validationResult
        if (!regex.matcher(value).matches()) {
            validationResult = new CustomValidationResult(false, msg)
        } else {
            validationResult = new CustomValidationResult(true)
        }
        log.debug("CustomValidationResult: \nvalid [${validationResult.valid}]\nvalue [${value}]\nregex [${regex}]")
        return validationResult;
    }
}
