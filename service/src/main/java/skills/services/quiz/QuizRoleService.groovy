package skills.services.quiz

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import skills.auth.UserNameService
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.controller.result.model.UserRoleRes
import skills.services.AccessSettingsStorageService
import skills.storage.model.QuizDef
import skills.storage.model.auth.RoleName
import skills.storage.repos.QuizDefRepo

import javax.transaction.Transactional

@Service
@Slf4j
class QuizRoleService {
    private static Set<RoleName> quizSupportedRoles = [RoleName.ROLE_QUIZ_ADMIN].toSet()

    @Autowired
    AccessSettingsStorageService accessSettingsStorageService

    @Autowired
    skills.auth.UserInfoService userInfoService

    @Autowired
    UserNameService userNameService

    @Autowired
    QuizDefRepo quizDefRepo

    @Transactional
    void addQuizRole(String userIdParam, String quizId, RoleName roleName) {
        QuizDef quizDef = findQuizDef(quizId)
        ensureValidRole(roleName, quizDef.quizId)
        String userId = userNameService.normalizeUserId(userIdParam)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillQuizException("Cannot add roles to myself. userId=[${userId}]", quizId, ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.addQuizDefUserRole(userId, quizDef.quizId, roleName)
    }

    List<UserRoleRes> getQuizUserRoles(String quizId) {
        QuizDef quizDef = findQuizDef(quizId)
        return accessSettingsStorageService.findAllQuizRoles(quizDef.quizId)
    }

    @Transactional
    void deleteQuizRole(String userIdParam, String quizId, RoleName roleName) {
        QuizDef quizDef = findQuizDef(quizId)
        ensureValidRole(roleName, quizDef.quizId)
        String userId = userNameService.normalizeUserId(userIdParam)
        String currentUser = userInfoService.getCurrentUserId()
        if (currentUser?.toLowerCase() == userId?.toLowerCase()) {
            throw new SkillQuizException("Cannot remove roles from myself. userId=[${userId}]", quizId, ErrorCode.AccessDenied)
        }
        accessSettingsStorageService.deleteQuizUserRole(userId, quizDef.quizId, roleName)
    }

    private void ensureValidRole(RoleName roleName, String quizId) {
        if (!quizSupportedRoles.contains(roleName)) {
            throw new SkillQuizException("Provided [${roleName}] is not a quiz role.", quizId, ErrorCode.BadParam)
        }
    }

    private QuizDef findQuizDef(String quizId) {
        QuizDef updatedDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!updatedDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return updatedDef
    }
}
