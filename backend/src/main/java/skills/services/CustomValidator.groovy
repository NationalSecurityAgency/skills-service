package skills.services

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SubjectRequest

import javax.annotation.PostConstruct
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
        return validateName(projectRequest.name)
    }

    CustomValidationResult validate(SubjectRequest subjectRequest) {
        return validateDescriptionAndName(subjectRequest.description, subjectRequest.name)
    }

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

        // split if
        // - there is at least 2 new lines
        // - markdown separator (3 underscores, 3 dashes, 3 stars)
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

    private String adjustForMarkdownSupport(String s) {

        String toValidate = s.trim()
        // remove a single newline so the provided regex does not need check for newlines themselves
        // since regex . doesn't match \n
        // this important since description allows the use of markdown, for example is an input for the markdown list:
        // "some paragraph\n*item1 *item2
        toValidate = toValidate.replaceAll("\n", "")

        // support markdown headers and blockquotes
        // # Header
        // ## Header
        // ### Header
        // > quote
        toValidate = toValidate.replaceAll("^[#>]{1,}", "")

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
        return validationResult;
    }
}
