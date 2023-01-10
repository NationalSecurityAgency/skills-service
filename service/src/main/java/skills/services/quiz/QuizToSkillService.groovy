package skills.services.quiz

import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillQuizException
import skills.storage.model.QuizDef
import skills.storage.model.QuizToSkillDef
import skills.storage.repos.QuizDefRepo
import skills.storage.repos.QuizToSkillDefRepo

@Service
@Slf4j
class QuizToSkillService {

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    QuizToSkillDefRepo quizToSkillDefRepo

    @Transactional
    void saveQuizToSkillAssignment(Integer skillRef, String quizId) {
        QuizDef quizDef = getQuizDef(quizId)

        QuizToSkillDef quizToSkillDef = quizToSkillDefRepo.findByQuizRefIdAndSkillRefId(quizDef.id, skillRef)
        if (!quizToSkillDef) {
            quizToSkillDef = new QuizToSkillDef(quizRefId: quizDef.id, skillRefId: skillRef)
            quizToSkillDefRepo.save(quizToSkillDef)
        }
    }

    QuizToSkillDefRepo.QuizNameAndId getQuizIdForSkillRefId(Integer skillRefId) {
        return quizToSkillDefRepo.getQuizIdBySkillIdRef(skillRefId)
    }

    private QuizDef getQuizDef(String quizId) {
        QuizDef quizDef = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (!quizDef) {
            throw new SkillQuizException("Failed to find quiz id.", quizId, ErrorCode.BadParam)
        }
        return quizDef
    }
}
