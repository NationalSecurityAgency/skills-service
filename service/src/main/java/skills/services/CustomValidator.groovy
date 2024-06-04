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
import jakarta.annotation.PostConstruct
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.auth.UserInfoService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.request.model.*
import skills.services.admin.UserCommunityService
import skills.utils.InputSanitizer

import java.util.regex.Matcher
import java.util.regex.Pattern

@Slf4j
@Service
class CustomValidator {

    @Value('#{"${skills.config.ui.paragraphValidationRegex}"}')
    String paragraphValidationRegex

    @Value('#{"${skills.config.ui.userCommunityParagraphValidationRegex}"}')
    String userCommunityParagraphValidationRegex

    @Value('#{"${skills.config.ui.forceValidationRegex:null}"}')
    String forceValidationRegex

    @Value('#{"${skills.config.ui.paragraphValidationMessage}"}')
    String paragraphValidationMessage

    @Value('#{"${skills.config.ui.userCommunityParagraphValidationMessage}"}')
    String userCommunityParagraphValidationMessage

    @Value('#{"${skills.config.ui.nameValidationRegex}"}')
    String nameValidationRegex

    @Value('#{"${skills.config.ui.nameValidationMessage}"}')
    String nameValidationMessage

    @Autowired
    UserCommunityService userCommunityService

    @Autowired
    UserInfoService userInfoService

    private String paragraphValidationMsg
    private String userCommunityParagraphValidationMsg
    private Pattern paragraphPattern
    private Pattern userCommunityParagraphPattern
    private Pattern forceValidationPattern

    private String nameValidationMsg
    private Pattern nameRegex

    private static final Pattern BULLET = ~/^\s*(?:\d\. |\* |- )/
    private static final Pattern NEWLINE = ~/\n/
    private static final Pattern HEADER_OR_BLOCK_QUOTE = ~/^([\n]?[#>]{1,}[\s])+/
    private static final Pattern BOLD_AND_ITALICS = ~/^(\s*)[*_]{1,3}([^*_]+)[*_]{1,3}/
    private static final Pattern HTML = ~/(?s)<[\/]?\w+(?: .+?)*>/
    private static final Pattern CODEBLOCK = ~/(?ms)(^[`]{3}$.*?^[`]{3}$)/

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

        userCommunityParagraphValidationMsg = userCommunityParagraphValidationMessage ?: "Description failed validation"
        if ( StringUtils.isNotBlank(userCommunityParagraphValidationRegex)){
            log.info("Configuring user community paragraph validator. regex=[{}], message=[{}]", userCommunityParagraphValidationRegex, userCommunityParagraphValidationMessage)
            userCommunityParagraphPattern = Pattern.compile(userCommunityParagraphValidationRegex)
        }

        if ( StringUtils.isNotBlank(forceValidationRegex)){
            log.info("Configuring paragraph force validator. regex=[{}]", forceValidationRegex)
            forceValidationPattern = Pattern.compile(forceValidationRegex)
        }

        nameValidationMsg = nameValidationMessage ?: "Name failed validation"
        if ( StringUtils.isNotBlank(nameValidationRegex)) {
            log.info("Configuring name validator. regex=[{}], message=[{}]", nameValidationRegex, nameValidationMsg)
            nameRegex = Pattern.compile(nameValidationRegex)
        }

        return this
    }

    CustomValidationResult validate(ProjectRequest projectRequest) {
        CustomValidationResult validationResult = validateDescription(projectRequest.description, projectRequest.projectId)
        if (!validationResult.valid) {
            return validationResult
        }

        validationResult = validateName(projectRequest.name)
        return validationResult
    }

    CustomValidationResult validate(SubjectRequest subjectRequest, String projectId) {
        return validateDescriptionAndName(subjectRequest.description, subjectRequest.name, projectId)
    }

    CustomValidationResult validate(QuizDefRequest quizDefRequest) {
        return validateDescriptionAndName(quizDefRequest.description, quizDefRequest.name)
    }

    @Profile
    CustomValidationResult validate(SkillRequest skillRequest) {
        return validateDescriptionAndName(skillRequest.description, skillRequest.name, skillRequest.projectId)
    }

    CustomValidationResult validate(BadgeRequest badgeRequest, String projectId) {
        return validateDescriptionAndName(badgeRequest.description, badgeRequest.name, projectId)
    }

    private CustomValidationResult validateDescriptionAndName(String description, String name, String projectId=null) {
        CustomValidationResult validationResult = validateDescription(description, projectId)
        if (!validationResult.valid) {
            return validationResult
        }

        validationResult = validateName(name)
        return validationResult
    }

    CustomValidationResult validateDescription(String description, String projectId=null, Boolean utilizeUserCommunityParagraphPatternByDefault = false) {
        Pattern paragraphPatternToUse = this.paragraphPattern
        String paragraphValidationMsgToUse = this.paragraphValidationMsg
        if ((utilizeUserCommunityParagraphPatternByDefault || projectId) && this.userCommunityParagraphPattern) {
            boolean shouldUseCommunityValidation = projectId ? userCommunityService.isUserCommunityOnlyProject(projectId) : utilizeUserCommunityParagraphPatternByDefault
            if (shouldUseCommunityValidation) {
                paragraphPatternToUse = this.userCommunityParagraphPattern
                paragraphValidationMsgToUse = this.userCommunityParagraphValidationMsg ?: this.paragraphValidationMsg

                String userId = userInfoService.currentUserId
                if (!userCommunityService.isUserCommunityMember(userId)) {
                    throw new SkillException("User [${userId}] is not allowed to validate using user community validation", projectId, null, ErrorCode.AccessDenied)
                }
            }
        }
        if (!paragraphPatternToUse || StringUtils.isBlank(description)) {
            return new CustomValidationResult(valid: true)
        }
        log.debug("Validating description:\n[{}]", description)

        description = InputSanitizer.unsanitizeForMarkdown(description)

        // split if
        // - there is at least 2 new lines
        // - markdown separator (3 underscores, 3 dashes, 3 stars)
        description = preProcessForMarkdownSupport(description)
        if (StringUtils.isBlank(description)) {
            return new CustomValidationResult(valid: true)
        }

        CustomValidationResult validationResult = null

        String[] paragraphs = description.split("([\n]{2,})|(\n[\\s]*[-_*]{3,})")
        if (paragraphs) {
            validationResult = validateParagraphs(paragraphPatternToUse, paragraphValidationMsgToUse, paragraphs.toList())
            if (validationResult && !validationResult.valid) {
                return validationResult
            }
        }

        List<String> htmlParagraphs = extractHtmlParagraphs(description)
        if (htmlParagraphs) {
            validationResult = validateParagraphs(paragraphPatternToUse, paragraphValidationMsgToUse, htmlParagraphs)
        }
        return validationResult
    }

    CustomValidationResult validateEmailBodyAndSubject(ContactUsersRequest contactUsersRequest) {
        CustomValidationResult res = validateDescription(contactUsersRequest.emailBody, null)
        if (!res.valid) {
            res.msg = "Custom validation failed: msg=[${res.msg}] for email's body"
            return res
        }
        res = validateDescription(contactUsersRequest.emailSubject, null)
        if (!res.valid) {
            res.msg = "Custom validation failed: msg=[${res.msg}] for email's subject'"
            return res
        }
        return res
    }

    private CustomValidationResult validateParagraphs(Pattern paragraphPatternToUse, String paragraphValidationMsgToUse, List<String> paragraphs){
        CustomValidationResult validationResult = null
        for (String s : paragraphs) {
            if (!s){
                continue
            }
            String toValidate = adjustForMarkdownSupport(s)
            if (StringUtils.isNotBlank(toValidate)) {
                validationResult = validateInternal(paragraphPatternToUse, toValidate, paragraphValidationMsgToUse)
                if (!validationResult.valid) {
                    break
                }
            } else if(!validationResult) {
                validationResult = new CustomValidationResult(valid: true)
            }
        }

        return validationResult
    }

    final static Pattern HTML_PARAGRAPHS_PATTERN = Pattern.compile("<p>(.+?)</p>", Pattern.DOTALL);
    private List<String> extractHtmlParagraphs(String description) {
        Matcher matcher = HTML_PARAGRAPHS_PATTERN.matcher(description)
        List<String> htmlParagraphs = []
        while (matcher.find()) {
            htmlParagraphs.add(matcher.group(1));
        }
        return htmlParagraphs
    }

    private String preProcessForMarkdownSupport(String toValidate) {
//        String toValidate = s.trim()

        // treat all linebreaks and separators as newlines
        toValidate = toValidate.replaceAll(/(?m)(^\s*[-_*]{3,}\s*$)|(^\s*<br>\s*$)/, '\n')

        // remove a single new line above a table and/or codeblock
        toValidate = TABLE_FIX.matcher(toValidate).replaceAll('$2')
        toValidate = CODEBLOCK_FIX.matcher(toValidate).replaceAll('$2')
        toValidate = LIST_FIX.matcher(toValidate).replaceAll('$2')

        // remove two+ newlines from codeblocks so we do not split
        StringBuilder out = new StringBuilder()
        Matcher matcher = CODEBLOCK.matcher(toValidate)
        while(matcher.find()) {
            matcher.appendReplacement(out, matcher.group(1).replaceAll(/[\n]{2,}+/, '\n'))
        }
        matcher.appendTail(out)
        toValidate = out.toString()

        return toValidate.trim()
    }

    private String adjustForMarkdownSupport(String s) {

        String toValidate = s.trim()

        // remove extra html markdown sometimes added by wysiwyg editor
        toValidate = HTML.matcher(toValidate).replaceAll("")

        // remove markdown bullets that start at the beginning of a line/paragraph
        toValidate = BULLET.matcher(toValidate).replaceAll("")

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

        // check if there are embedded new lines
        if (forceValidationPattern && (value =~ /\n/) ) {
            String[] paragraphs = value.split("([\n])")
            for (String s : paragraphs) {
                if (!s) { continue }
                String toValidate = adjustForMarkdownSupport(s)
                if (forceValidationPattern.matcher(toValidate).matches()) {
                    if (!regex.matcher(toValidate).matches()) {
                        validationResult = new CustomValidationResult(false, msg)
                    } else {
                        validationResult = new CustomValidationResult(true)
                    }
                    if (!validationResult.valid) {
                        break
                    }
                }
            }
        }

        if (!validationResult || validationResult.isValid()) {
            // remove a single newline so the provided regex does not need check for newlines themselves
            // since regex . doesn't match \n
            // this important since description allows the use of markdown, for example is an input for the markdown list:
            // "some paragraph\n*item1 *item2
            value = NEWLINE.matcher(value).replaceAll("")

            if (!regex.matcher(value).matches()) {
                validationResult = new CustomValidationResult(false, msg)
            } else {
                validationResult = new CustomValidationResult(true)
            }
        }
        log.debug("CustomValidationResult: \nvalid [${validationResult.valid}]\nvalue [${value}]\nregex [${regex}]")
        return validationResult;
    }
}
