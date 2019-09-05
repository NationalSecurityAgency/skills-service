package skills.services

import groovy.util.logging.Slf4j
import org.apache.commons.lang3.StringUtils
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import skills.controller.request.model.BadgeRequest
import skills.controller.request.model.ProjectRequest
import skills.controller.request.model.SkillRequest
import skills.controller.request.model.SubjectRequest

import java.util.regex.Pattern

@Slf4j
@Service
class CustomValidator {

    private Map<String, Pattern> regexCache = [:]

    @Value('#{"${skills.config.ui.paragraphValidationRegex}"}')
    String paragraphValidationRegex

    @Value('#{"${skills.config.ui.paragraphValidationMessage}"}')
    String paragraphValidationMessage

    @Value('#{"${skills.config.ui.nameValidationRegex}"}')
    String nameValidationRegex

    @Value('#{"${skills.config.ui.nameValidationMessage}"}')
    String nameValidationMessage

    public CustomValidationResult validate(ProjectRequest projectRequest) {
        return validateName(projectRequest.name)
    }

    public CustomValidationResult validate(SubjectRequest subjectRequest) {
        return validateDescriptionAndName(subjectRequest.description, subjectRequest.name)
    }

    public CustomValidationResult validate(SkillRequest skillRequest) {
        return validateDescriptionAndName(skillRequest.description, skillRequest.name)
    }

    public CustomValidationResult validate(BadgeRequest badgeRequest) {
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

    public CustomValidationResult validateDescription(String description) {
        if (StringUtils.isBlank(paragraphValidationRegex) || description == null) {
            return new CustomValidationResult(valid: true)
        }

        String[] paragraphs = description.split("\n\n")
        Pattern pattern = compileRegex(paragraphValidationRegex)

        CustomValidationResult validationResult = null
        for (String s : paragraphs) {
            validationResult = validateInternal(pattern, s.trim(), paragraphValidationMessage ?: "Description failed validation")
            if (!validationResult.valid) {
                break
            }
        }

        return validationResult
    }

    public CustomValidationResult validateName(String name) {
        if (StringUtils.isBlank(nameValidationRegex) || name == null) {
            return new CustomValidationResult(valid: true)
        }

        Pattern regex = compileRegex(nameValidationRegex)
        CustomValidationResult validationResult = validateInternal(regex, name, nameValidationMessage ?: "Name failed validation")
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

    private Pattern compileRegex(String regex) {
        if (!regexCache.containsKey(regex)) {
            regexCache.put(regex, Pattern.compile(regex))
        }

        return regexCache.get(regex)
    }
}
