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
package skills.services.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import skills.controller.exceptions.ErrorCode
import skills.controller.exceptions.SkillException
import skills.controller.exceptions.SkillQuizException
import skills.storage.model.AdminGroupDef
import skills.storage.model.ProjDef
import skills.storage.model.QuizDef
import skills.storage.repos.AdminGroupDefRepo
import skills.storage.repos.ProjDefRepo
import skills.storage.repos.QuizDefRepo

@Component
class ServiceValidatorHelper {

    @Autowired
    ProjDefRepo projDefRepo

    @Autowired
    QuizDefRepo quizDefRepo

    @Autowired
    AdminGroupDefRepo adminGroupDefRepo

    void validateProjectIdDoesNotExist(String projectId) {
        ProjDef idExist = projDefRepo.findByProjectIdIgnoreCase(projectId)
        if (idExist) {
            throw new SkillException("Project with id [${projectId}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
        }
    }

    void validateProjectNameDoesNotExist(String projectName, String projectId) {
        ProjDef nameExist = projDefRepo.findByNameIgnoreCase(projectName)
        if (nameExist) {
            throw new SkillException("Project with name [${projectName}] already exists! Sorry!", projectId, null, ErrorCode.ConstraintViolation)
        }
    }

    void validateQuizIdDoesNotExist(String quizId) {
        QuizDef idExist = quizDefRepo.findByQuizIdIgnoreCase(quizId)
        if (idExist) {
            throw new SkillQuizException("Quiz with id [${quizId}] already exists! Sorry!", quizId, ErrorCode.ConstraintViolation)
        }
    }

    void validateQuizNameDoesNotExist(String quizName, String quizId) {
        QuizDef nameExist = quizDefRepo.findByNameIgnoreCase(quizName)
        if (nameExist) {
            throw new SkillQuizException("Quiz with name [${quizName}] already exists! Sorry!", quizId, ErrorCode.ConstraintViolation)
        }
    }

    void validateAdminGroupIdDoesNotExist(String adminGroupId) {
        AdminGroupDef idExist = adminGroupDefRepo.findByAdminGroupIdIgnoreCase(adminGroupId)
        if (idExist) {
            throw new SkillException("Admin Group with id [${adminGroupId}] already exists! Sorry!",  ErrorCode.ConstraintViolation)
        }
    }

    void validateAdminGroupNameDoesNotExist(String adminGroupName, String adminGroupId) {
        AdminGroupDef nameExist = adminGroupDefRepo.findByNameIgnoreCase(adminGroupName)
        if (nameExist) {
            throw new SkillException("Admin Group with name [${adminGroupName}] already exists! Sorry!", ErrorCode.ConstraintViolation)
        }
    }

}
