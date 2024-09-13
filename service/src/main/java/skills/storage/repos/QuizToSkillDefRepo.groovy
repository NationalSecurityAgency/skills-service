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
package skills.storage.repos

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.lang.Nullable
import skills.controller.result.model.QuizSkillResult
import skills.storage.model.QuizDefParent
import skills.storage.model.QuizToSkillDef
import skills.storage.model.SkillDef
import skills.storage.model.SkillDefSkinny
import skills.storage.model.SubjectAwareSkillDef

interface QuizToSkillDefRepo extends JpaRepository<QuizToSkillDef, Long> {

    static interface QuizNameAndId {
        Integer getSkillRefId()
        String getQuizName()
        String getQuizId()
        QuizDefParent.QuizType getQuizType()
        Integer getNumQuestions()
    }

    static interface ProjectIdAndSkillId {
        Integer getSkillRefId()
        String getProjectId()
        String getSkillId()
    }

    @Nullable
    @Query(value = '''select q.quiz_id as quizId,
                   max(q.name) as quizName,
                   max(q.type) as quizType,
                   max(qToS.skill_ref_id) as skillRefId,
                   count(question.id) as numQuestions
            from quiz_to_skill_definition qToS, quiz_definition q
                 left join quiz_question_definition question on (q.quiz_id = question.quiz_id)
            where qToS.skill_ref_id = ?1
              and q.id = qToS.quiz_ref_id
            group by q.quiz_id''', nativeQuery = true)
    QuizNameAndId getQuizIdBySkillIdRef(Integer skillIdRef)

    @Nullable
    @Query('''select child.name as skillName, child.skillId as skillId, child.projectId as projectId, subject.skillId as subjectId,
                     subject.name as subjectName, exists(
                       select ur.roleName from UserRole ur where ((ur.projectId = child.projectId and
                       ur.roleName in ('ROLE_PROJECT_ADMIN', 'ROLE_PROJECT_APPROVER')) OR ur.roleName = 'ROLE_SUPER_DUPER_USER') and ur.userId = ?2
                   ) as canUserAccess, subject.totalPoints as subjectPoints, project.totalPoints as projectPoints
              from QuizToSkillDef quiz, SkillDefWithExtra child
              join SkillRelDef srd on srd.child.id = child.id and srd.type in ('RuleSetDefinition', 'GroupSkillToSubject')
              join SkillDef subject on subject = srd.parent and subject.type = 'Subject'
              join ProjDef project on project.projectId = subject.projectId
              where
                    quiz.quizRefId = ?1 AND
                    child.id = quiz.skillRefId AND
                    project.projectId = child.projectId
              order by projectId, skillName asc
    ''')
    List<QuizSkillResult> getSkillsForQuizWithSubjects(Integer quizRefId, String userId)

    @Nullable
    @Query('''select skill.id as skillRefId, skill.skillId as skillId, skill.projectId as projectId, skill.totalPoints as points
            from QuizToSkillDef qToS, SkillDef skill 
            where qToS.quizRefId = ?1
                and skill.id = qToS.skillRefId''')
    List<ProjectIdAndSkillId> getSkillsForQuiz(Integer quizRefId)

    @Nullable
    @Query(value = '''select q.quiz_id as quizId,
               max(q.name) as quizName,
               max(q.type) as quizType,
               qToS.skill_ref_id as skillRefId,
               count(question.id) as numQuestions
        from quiz_to_skill_definition qToS, quiz_definition q
             left join quiz_question_definition question on (q.quiz_id = question.quiz_id)
        where qToS.skill_ref_id in ?1
          and q.id = qToS.quiz_ref_id
        group by q.quiz_id, qToS.skill_ref_id, qToS.skill_ref_id, q.quiz_id''', nativeQuery = true)
    List<QuizNameAndId> getQuizInfoSkillIdRef(List<Integer> skillIdRef)

    void deleteBySkillRefId(Integer skillRefId)

    Integer countByQuizRefId(Integer quizRefId)

    @Query('''select count(quiz) > 0
            from QuizToSkillDef qToS, QuizDef  quiz, SkillDef  skill
            where quiz.id = qToS.quizRefId
                and skill.id = qToS.skillRefId
                and quiz.quizId = ?1
                and skill.projectId in ?2 ''')
    boolean existQuizIdToOneOfTheProjectIdsAssociation(String quizId, List<String> projectIds)

    @Nullable
    @Query('''select qToS.skillRefId
            from QuizToSkillDef qToS, SkillDef skill 
            where skill.projectId = ?1
                and skill.id = qToS.skillRefId''')
    List<Integer> getSkillRefIdsWithQuizByProjectId(String projectId)

    @Nullable
    @Query('''select qToS.quizRefId from QuizToSkillDef qToS where qToS.skillRefId = ?1''')
    List<Integer> getQuizRefIdsBySkillRefId(Integer skillRefId)

    @Nullable
    @Query(value = '''select otherProjSkill.id as skillRefId, otherProjSkill.project_id as projectId, otherProjSkill.skill_id as skillId
            from quiz_to_skill_definition qToSToOrigProj,
                 skill_definition skillInOrigProject,
                 quiz_to_skill_definition qToSToOtherProj,
                 skill_definition otherProjSkill
            where skillInOrigProject.project_id = ?1
              and otherProjSkill.project_id <> ?1
              and skillInOrigProject.id = qToSToOrigProj.skill_ref_id
              and qToSToOrigProj.quiz_ref_id = qToSToOtherProj.quiz_ref_id
            and otherProjSkill.id = qToSToOtherProj.skill_ref_id;''', nativeQuery = true)
    List<ProjectIdAndSkillId> getOtherProjectsSkillRefIdsWithQuizzesInThisProject(String projectId)
}

