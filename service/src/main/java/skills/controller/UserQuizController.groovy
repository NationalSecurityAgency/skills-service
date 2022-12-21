package skills.controller

import callStack.profiler.Profile
import groovy.transform.CompileStatic
import groovy.util.logging.Slf4j
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.CrossOrigin
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import org.springframework.web.bind.annotation.RestController
import skills.auth.UserInfoService
import skills.auth.aop.AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
import skills.quizLoading.QuizRunService
import skills.quizLoading.model.QuizInfo
import skills.skillLoading.model.OverallSkillSummary

import javax.servlet.http.HttpServletRequest

@CrossOrigin(allowCredentials = "true", originPatterns = ["*"])
@RestController
@RequestMapping("/api")
@AdminOrApproverGetRequestUsersOnlyWhenUserIdSupplied
@skills.profile.EnableCallStackProf
@CompileStatic
@Slf4j
class UserQuizController {

    @Autowired
    QuizRunService quizRunService

    @RequestMapping(value = "/quizzes/{quizId}", method = RequestMethod.GET, produces = "application/json")
    @ResponseBody
    QuizInfo getQuizInfo(@PathVariable("quizId") String quizId) {
        return quizRunService.loadQuizInfo(quizId);
    }

}
