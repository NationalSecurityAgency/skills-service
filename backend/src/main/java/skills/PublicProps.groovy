package skills

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.UIConfigProperties

@Component
class PublicProps {

    enum UiProp {
        descriptionMaxLength,
        maxTimeWindowInMinutes,
        docsHost,
        maxProjectsPerAdmin,
        maxSubjectsPerProject,
        maxBadgesPerProject,
        maxSkillsPerSubject,
        paragraphValidationRegex,
        paragraphValidationMessage,
        nameValidationRegex,
        nameValidationMessage,
        maxFirstNameLength,
        maxLastNameLength,
        maxNicknameLength,
        minPasswordLength,
        maxPasswordLength,
        minNameLength,
        maxBadgeNameLength,
        maxProjectNameLength,
        maxSkillNameLength,
        maxSubjectNameLength,
        maxLevelNameLength,
        minIdLength,
        maxIdLength,
        maxSkillVersion,
        maxPointIncrement,
        maxNumPerformToCompletion,
        maxNumPointIncrementMaxOccurrences,
    }

    @Autowired
    UIConfigProperties uiConfigProperties

    def get(UiProp prop) {
        def res = uiConfigProperties.ui."${prop.name()}"
        if (res == null) {
            throw new IllegalArgumentException("Failed to find property ${prop}")
        }
        return res
    }

    int getInt(UiProp prop) {
        Object res = get(prop)
        return res instanceof String ? res.toInteger() : (Integer)res
    }
}
