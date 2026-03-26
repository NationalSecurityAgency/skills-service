/**
 * Copyright 2026 SkillTree
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
package skills.intTests.metrics.global

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.QuizDefFactory
import skills.intTests.utils.SkillsClientException
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName

import static skills.metrics.GlobalProgressMetricsService.getUSER_PREF_GLOBAL_METRICS_EXCLUSION

class GlobalUserSettingsSpecs extends DefaultIntSpec {

    def "one user with settings and with none" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}
        def proj1 = SkillsFactory.createProject(1)

        users[0].createProject(proj1)
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [setting: USER_PREF_GLOBAL_METRICS_EXCLUSION, value: true, projectId: proj1.projectId ],
        ])
        users[0].addProjectAdmin(proj1.projectId, users[1].userName)
        when:
        def u1REs = users[0].getGlobalMetricsUserSettings(USER_PREF_GLOBAL_METRICS_EXCLUSION)
        def u2Res = users[1].getGlobalMetricsUserSettings(USER_PREF_GLOBAL_METRICS_EXCLUSION)
        then:
        u1REs.projectId == [proj1.projectId]
        u1REs.quizId == [null]
        u1REs.setting == [USER_PREF_GLOBAL_METRICS_EXCLUSION]
        u1REs.value == ["true"]

        u2Res == []
    }

    def "users with multiple project settings" () {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        List projects = (1..4).collect {
            def proj = SkillsFactory.createProject(it)
            users[0].createProject(proj)

            users[0].addProjectAdmin(proj.projectId, users[1].userName)
            users[0].addProjectAdmin(proj.projectId, users[2].userName)
            return proj
        }

        List<String> settingNames = ["one", "two", "three", "four"]
        List<String> settingValues = ["five", "six", "seven", "eight"]
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[0], projectId: projects[0].projectId ],
                [ setting  : settingNames[1], value    : settingValues[1], projectId: projects[0].projectId ],
                [ setting  : settingNames[2], value    : settingValues[0], projectId: projects[0].projectId ],

                [ setting  : settingNames[0], value    : settingValues[1], projectId: projects[1].projectId ],
                [ setting  : settingNames[1], value    : settingValues[1], projectId: projects[1].projectId ],

                [ setting  : settingNames[0], value    : settingValues[2], projectId: projects[2].projectId ],

                [ setting  : settingNames[1], value    : settingValues[2], projectId: projects[3].projectId ],
        ])

        users[1].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[2], projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : settingValues[3], projectId: projects[2].projectId ],

                [ setting  : settingNames[3], value    : settingValues[1], projectId: projects[0].projectId ],
        ])

        users[2].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[2], value    : settingValues[0], projectId: projects[1].projectId ],
        ])

        when:
        def u1REs_setting1 = users[0].getGlobalMetricsUserSettings(settingNames[0]).sort { it.projectId }
        def u1REs_setting2 = users[0].getGlobalMetricsUserSettings(settingNames[1]).sort { it.projectId }
        def u1REs_setting3 = users[0].getGlobalMetricsUserSettings(settingNames[2]).sort { it.projectId }
        def u1REs_setting4 = users[0].getGlobalMetricsUserSettings(settingNames[3]).sort { it.projectId }

        def u2REs_setting1 = users[1].getGlobalMetricsUserSettings(settingNames[0]).sort { it.projectId }
        def u2REs_setting2 = users[1].getGlobalMetricsUserSettings(settingNames[1]).sort { it.projectId }
        def u2REs_setting3 = users[1].getGlobalMetricsUserSettings(settingNames[2]).sort { it.projectId }
        def u2REs_setting4 = users[1].getGlobalMetricsUserSettings(settingNames[3]).sort { it.projectId }

        def u3REs_setting1 = users[2].getGlobalMetricsUserSettings(settingNames[0]).sort { it.projectId }
        def u3REs_setting2 = users[2].getGlobalMetricsUserSettings(settingNames[1]).sort { it.projectId }
        def u3REs_setting3 = users[2].getGlobalMetricsUserSettings(settingNames[2]).sort { it.projectId }
        def u3REs_setting4 = users[2].getGlobalMetricsUserSettings(settingNames[3]).sort { it.projectId }
        then:
        // user 1
        u1REs_setting1.projectId == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
        u1REs_setting1.quizId == [null, null, null]
        u1REs_setting1.setting == [settingNames[0], settingNames[0], settingNames[0]]
        u1REs_setting1.value == [settingValues[0], settingValues[1], settingValues[2]]

        u1REs_setting2.projectId == [projects[0].projectId, projects[1].projectId, projects[3].projectId]
        u1REs_setting2.quizId == [null, null, null]
        u1REs_setting2.setting == [settingNames[1], settingNames[1], settingNames[1]]
        u1REs_setting2.value == [settingValues[1], settingValues[1], settingValues[2]]

        u1REs_setting3.projectId == [projects[0].projectId]
        u1REs_setting3.quizId == [null]
        u1REs_setting3.setting == [settingNames[2]]
        u1REs_setting3.value == [settingValues[0]]

        u1REs_setting4 == []

        // user 2
        u2REs_setting1.projectId == [projects[0].projectId, projects[2].projectId]
        u2REs_setting1.quizId == [null, null]
        u2REs_setting1.setting == [settingNames[0], settingNames[0]]
        u2REs_setting1.value == [settingValues[2], settingValues[3]]

        u2REs_setting2 == []
        u2REs_setting3 == []

        u2REs_setting4.projectId == [projects[0].projectId]
        u2REs_setting4.quizId == [null]
        u2REs_setting4.setting == [settingNames[3]]
        u2REs_setting4.value == [settingValues[1]]

        // user 3
        u3REs_setting1 == []
        u3REs_setting2 == []

        u3REs_setting3.projectId == [projects[1].projectId]
        u3REs_setting3.quizId == [null]
        u3REs_setting3.setting == [settingNames[2]]
        u3REs_setting3.value == [settingValues[0]]

        u3REs_setting4 == []
    }

    def "users with multiple quiz settings" () {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        List quizzes = (1..4).collect {
            def quiz = QuizDefFactory.createQuiz(it)
            users[0].createQuizDef(quiz)

            users[0].addQuizUserRole(quiz.quizId, users[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            users[0].addQuizUserRole(quiz.quizId, users[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            return quiz
        }

        List<String> settingNames = ["one", "two", "three", "four"]
        List<String> settingValues = ["five", "six", "seven", "eight"]
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[0], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[1], value    : settingValues[1], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[2], value    : settingValues[0], quizId: quizzes[0].quizId ],

                [ setting  : settingNames[0], value    : settingValues[1], quizId: quizzes[1].quizId ],
                [ setting  : settingNames[1], value    : settingValues[1], quizId: quizzes[1].quizId ],

                [ setting  : settingNames[0], value    : settingValues[2], quizId: quizzes[2].quizId ],

                [ setting  : settingNames[1], value    : settingValues[2], quizId: quizzes[3].quizId ],
        ])

        users[1].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[2], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : settingValues[3], quizId: quizzes[2].quizId ],

                [ setting  : settingNames[3], value    : settingValues[1], quizId: quizzes[0].quizId ],
        ])

        users[2].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[2], value    : settingValues[0], quizId: quizzes[1].quizId ],
        ])

        when:
        def u1REs_setting1 = users[0].getGlobalMetricsUserSettings(settingNames[0]).sort { it.quizId }
        def u1REs_setting2 = users[0].getGlobalMetricsUserSettings(settingNames[1]).sort { it.quizId }
        def u1REs_setting3 = users[0].getGlobalMetricsUserSettings(settingNames[2]).sort { it.quizId }
        def u1REs_setting4 = users[0].getGlobalMetricsUserSettings(settingNames[3]).sort { it.quizId }

        def u2REs_setting1 = users[1].getGlobalMetricsUserSettings(settingNames[0]).sort { it.quizId }
        def u2REs_setting2 = users[1].getGlobalMetricsUserSettings(settingNames[1]).sort { it.quizId }
        def u2REs_setting3 = users[1].getGlobalMetricsUserSettings(settingNames[2]).sort { it.quizId }
        def u2REs_setting4 = users[1].getGlobalMetricsUserSettings(settingNames[3]).sort { it.quizId }

        def u3REs_setting1 = users[2].getGlobalMetricsUserSettings(settingNames[0]).sort { it.quizId }
        def u3REs_setting2 = users[2].getGlobalMetricsUserSettings(settingNames[1]).sort { it.quizId }
        def u3REs_setting3 = users[2].getGlobalMetricsUserSettings(settingNames[2]).sort { it.quizId }
        def u3REs_setting4 = users[2].getGlobalMetricsUserSettings(settingNames[3]).sort { it.quizId }
        then:
        // user 1
        u1REs_setting1.quizId == [quizzes[0].quizId, quizzes[1].quizId, quizzes[2].quizId]
        u1REs_setting1.projectId == [null, null, null]
        u1REs_setting1.setting == [settingNames[0], settingNames[0], settingNames[0]]
        u1REs_setting1.value == [settingValues[0], settingValues[1], settingValues[2]]

        u1REs_setting2.quizId == [quizzes[0].quizId, quizzes[1].quizId, quizzes[3].quizId]
        u1REs_setting2.projectId == [null, null, null]
        u1REs_setting2.setting == [settingNames[1], settingNames[1], settingNames[1]]
        u1REs_setting2.value == [settingValues[1], settingValues[1], settingValues[2]]

        u1REs_setting3.quizId == [quizzes[0].quizId]
        u1REs_setting3.projectId == [null]
        u1REs_setting3.setting == [settingNames[2]]
        u1REs_setting3.value == [settingValues[0]]

        u1REs_setting4 == []

        // user 2
        u2REs_setting1.quizId == [quizzes[0].quizId, quizzes[2].quizId]
        u2REs_setting1.projectId == [null, null]
        u2REs_setting1.setting == [settingNames[0], settingNames[0]]
        u2REs_setting1.value == [settingValues[2], settingValues[3]]

        u2REs_setting2 == []
        u2REs_setting3 == []

        u2REs_setting4.quizId == [quizzes[0].quizId]
        u2REs_setting4.projectId == [null]
        u2REs_setting4.setting == [settingNames[3]]
        u2REs_setting4.value == [settingValues[1]]

        // user 3
        u3REs_setting1 == []
        u3REs_setting2 == []

        u3REs_setting3.quizId == [quizzes[1].quizId]
        u3REs_setting3.projectId == [null]
        u3REs_setting3.setting == [settingNames[2]]
        u3REs_setting3.value == [settingValues[0]]

        u3REs_setting4 == []
    }

    def "users with multiple project and quiz settings" () {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        List projects = (1..4).collect {
            def proj = SkillsFactory.createProject(it)
            users[0].createProject(proj)

            users[0].addProjectAdmin(proj.projectId, users[1].userName)
            users[0].addProjectAdmin(proj.projectId, users[2].userName)
            return proj
        }

        List quizzes = (1..4).collect {
            def quiz = QuizDefFactory.createQuiz(it)
            users[0].createQuizDef(quiz)

            users[0].addQuizUserRole(quiz.quizId, users[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            users[0].addQuizUserRole(quiz.quizId, users[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            return quiz
        }

        List<String> settingNames = ["one", "two", "three", "four"]
        List<String> settingValues = ["five", "six", "seven", "eight"]
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[0], projectId: projects[0].projectId ],
                [ setting  : settingNames[1], value    : settingValues[1], projectId: projects[0].projectId ],
                [ setting  : settingNames[2], value    : settingValues[0], projectId: projects[0].projectId ],

                [ setting  : settingNames[0], value    : settingValues[1], projectId: projects[1].projectId ],
                [ setting  : settingNames[1], value    : settingValues[1], projectId: projects[1].projectId ],

                [ setting  : settingNames[0], value    : settingValues[2], projectId: projects[2].projectId ],

                [ setting  : settingNames[1], value    : settingValues[2], projectId: projects[3].projectId ],

                [ setting  : settingNames[0], value    : settingValues[0], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[1], value    : settingValues[1], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[2], value    : settingValues[0], quizId: quizzes[0].quizId ],

                [ setting  : settingNames[0], value    : settingValues[1], quizId: quizzes[1].quizId ],
                [ setting  : settingNames[1], value    : settingValues[1], quizId: quizzes[1].quizId ],

                [ setting  : settingNames[0], value    : settingValues[2], quizId: quizzes[2].quizId ],

                [ setting  : settingNames[1], value    : settingValues[2], quizId: quizzes[3].quizId ],
        ])

        users[1].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[2], projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : settingValues[3], projectId: projects[2].projectId ],

                [ setting  : settingNames[3], value    : settingValues[1], projectId: projects[0].projectId ],

                [ setting  : settingNames[0], value    : settingValues[2], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : settingValues[3], quizId: quizzes[2].quizId ],

                [ setting  : settingNames[3], value    : settingValues[1], quizId: quizzes[0].quizId ],
        ])

        users[2].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[2], value    : settingValues[0], projectId: projects[1].projectId ],

                [ setting  : settingNames[2], value    : settingValues[0], quizId: quizzes[1].quizId ],
        ])

        Closure sorter = { a, b ->
                // First compare projectId, treating null as greater than any non-null value
                if (a.projectId == null && b.projectId == null) return 0
                if (a.projectId == null) return 1
                if (b.projectId == null) return -1
                def projCompare = a.projectId <=> b.projectId
                if (projCompare != 0) return projCompare

                // Then compare quizId, treating null as greater than any non-null value
                if (a.quizId == null && b.quizId == null) return 0
                if (a.quizId == null) return 1
                if (b.quizId == null) return -1
                return a.quizId <=> b.quizId
        }

        when:
        def u1REs_setting1 = users[0].getGlobalMetricsUserSettings(settingNames[0]).sort(sorter)
        def u1REs_setting2 = users[0].getGlobalMetricsUserSettings(settingNames[1]).sort(sorter)
        def u1REs_setting3 = users[0].getGlobalMetricsUserSettings(settingNames[2]).sort(sorter)
        def u1REs_setting4 = users[0].getGlobalMetricsUserSettings(settingNames[3]).sort(sorter)

        def u2REs_setting1 = users[1].getGlobalMetricsUserSettings(settingNames[0]).sort(sorter)
        def u2REs_setting2 = users[1].getGlobalMetricsUserSettings(settingNames[1]).sort(sorter)
        def u2REs_setting3 = users[1].getGlobalMetricsUserSettings(settingNames[2]).sort(sorter)
        def u2REs_setting4 = users[1].getGlobalMetricsUserSettings(settingNames[3]).sort(sorter)

        def u3REs_setting1 = users[2].getGlobalMetricsUserSettings(settingNames[0]).sort(sorter)
        def u3REs_setting2 = users[2].getGlobalMetricsUserSettings(settingNames[1]).sort(sorter)
        def u3REs_setting3 = users[2].getGlobalMetricsUserSettings(settingNames[2]).sort(sorter)
        def u3REs_setting4 = users[2].getGlobalMetricsUserSettings(settingNames[3]).sort(sorter)
        then:
        // user 1
        u1REs_setting1.quizId == [null, null, null, quizzes[0].quizId, quizzes[1].quizId, quizzes[2].quizId]
        u1REs_setting1.projectId == [projects[0].projectId, projects[1].projectId, projects[2].projectId, null, null, null]
        u1REs_setting1.setting == [settingNames[0], settingNames[0], settingNames[0], settingNames[0], settingNames[0], settingNames[0]]
        u1REs_setting1.value == [settingValues[0], settingValues[1], settingValues[2], settingValues[0], settingValues[1], settingValues[2]]

        u1REs_setting2.quizId == [null, null, null, quizzes[0].quizId, quizzes[1].quizId, quizzes[3].quizId]
        u1REs_setting2.projectId == [projects[0].projectId, projects[1].projectId, projects[3].projectId, null, null, null]
        u1REs_setting2.setting == [settingNames[1], settingNames[1], settingNames[1], settingNames[1], settingNames[1], settingNames[1]]
        u1REs_setting2.value == [settingValues[1], settingValues[1], settingValues[2], settingValues[1], settingValues[1], settingValues[2]]

        u1REs_setting3.quizId == [null, quizzes[0].quizId]
        u1REs_setting3.projectId == [projects[0].projectId, null]
        u1REs_setting3.setting == [settingNames[2], settingNames[2]]
        u1REs_setting3.value == [settingValues[0], settingValues[0]]

        u1REs_setting4 == []

        // user 2
        u2REs_setting1.quizId == [null, null, quizzes[0].quizId, quizzes[2].quizId]
        u2REs_setting1.projectId == [projects[0].projectId, projects[2].projectId, null, null]
        u2REs_setting1.setting == [settingNames[0], settingNames[0], settingNames[0], settingNames[0]]
        u2REs_setting1.value == [settingValues[2], settingValues[3], settingValues[2], settingValues[3]]

        u2REs_setting2 == []
        u2REs_setting3 == []

        u2REs_setting4.quizId == [null, quizzes[0].quizId]
        u2REs_setting4.projectId == [projects[0].projectId, null]
        u2REs_setting4.setting == [settingNames[3], settingNames[3]]
        u2REs_setting4.value == [settingValues[1], settingValues[1]]

        // user 3
        u3REs_setting1 == []
        u3REs_setting2 == []

        u3REs_setting3.quizId == [null, quizzes[1].quizId]
        u3REs_setting3.projectId == [projects[1].projectId, null]
        u3REs_setting3.setting == [settingNames[2], settingNames[2]]
        u3REs_setting3.value == [settingValues[0], settingValues[0]]

        u3REs_setting4 == []
    }

    def "must be a project admin in order to save a setting for a project" () {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        List projects = (1..3).collect {
            def proj = SkillsFactory.createProject(it)
            users[0].createProject(proj)
            users[0].addProjectAdmin(proj.projectId, users[1].userName)
            return proj
        }

        users[0].addProjectAdmin(projects[0].projectId, users[2].userName)
        users[0].addProjectAdmin(projects[1].projectId, users[2].userName)

        List<String> settingNames = ["one", "two", "three", "four"]
        List<String> settingValues = ["five", "six", "seven", "eight"]
        def settingsForThreeProjects = [
                [ setting  : settingNames[0], value    : settingValues[0], projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : settingValues[1], projectId: projects[1].projectId ],
                [ setting  : settingNames[0], value    : settingValues[2], projectId: projects[2].projectId ],
        ]
        users[0].addOrUpdateGlobalMetricsUserSettings(settingsForThreeProjects)
        users[1].addOrUpdateGlobalMetricsUserSettings(settingsForThreeProjects)
        when:
        users[2].addOrUpdateGlobalMetricsUserSettings(settingsForThreeProjects)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User must be an admin in order to save settings for this project")
        e.message.contains("projectId:${projects[2].projectId}")
    }

    def "must be a quiz admin in order to save a setting for q quiz" () {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        List quizzes = (1..4).collect {
            def quiz = QuizDefFactory.createQuiz(it)
            users[0].createQuizDef(quiz)

            users[0].addQuizUserRole(quiz.quizId, users[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            return quiz
        }

        users[0].addQuizUserRole(quizzes[0].quizId, users[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
        users[0].addQuizUserRole(quizzes[1].quizId, users[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())

        List<String> settingNames = ["one", "two", "three", "four"]
        List<String> settingValues = ["five", "six", "seven", "eight"]
        def settingsForThreeProjects = [
                [ setting  : settingNames[0], value    : settingValues[0], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : settingValues[1], quizId: quizzes[1].quizId ],
                [ setting  : settingNames[0], value    : settingValues[2], quizId: quizzes[2].quizId ],
        ]
        users[0].addOrUpdateGlobalMetricsUserSettings(settingsForThreeProjects)
        users[1].addOrUpdateGlobalMetricsUserSettings(settingsForThreeProjects)
        when:
        users[2].addOrUpdateGlobalMetricsUserSettings(settingsForThreeProjects)
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User must be an admin in order to save settings for this quiz")
        e.message.contains("quizId:${quizzes[2].quizId}")
    }

    def "project or quiz id must be provided" () {
        when:
        skillsService.addOrUpdateGlobalMetricsUserSettings([
                [ setting  : "one", value    : "two" ],
        ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Exactly one of projectId or quizId must be specified")
    }

    def "both project or quiz id must not be provided" () {
        when:
        skillsService.addOrUpdateGlobalMetricsUserSettings([
                [ setting  : "one", value    : "two", projectId: "1", quizId: "1" ],
        ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("Exactly one of projectId or quizId must be specified")
    }

    def "projectId does not exist" () {
        when:
        skillsService.addOrUpdateGlobalMetricsUserSettings([
                [ setting  : "one", value    : "two", projectId: "1" ],
        ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User must be an admin in order to save settings for this project")
    }

    def "quizId does not exist" () {
        when:
        skillsService.addOrUpdateGlobalMetricsUserSettings([
                [ setting  : "one", value    : "two", quizId: "1" ],
        ])
        then:
        SkillsClientException e = thrown(SkillsClientException)
        e.message.contains("User must be an admin in order to save settings for this quiz")
    }

    def "settings with empty value are removed" () {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        List projects = (1..4).collect {
            def proj = SkillsFactory.createProject(it)
            users[0].createProject(proj)

            users[0].addProjectAdmin(proj.projectId, users[1].userName)
            users[0].addProjectAdmin(proj.projectId, users[2].userName)
            return proj
        }

        List<String> settingNames = ["one", "two", "three", "four"]
        List<String> settingValues = ["five", "six", "seven", "eight"]
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[0], projectId: projects[0].projectId ],
                [ setting  : settingNames[1], value    : settingValues[1], projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : settingValues[1], projectId: projects[1].projectId ],
                [ setting  : settingNames[0], value    : settingValues[2], projectId: projects[2].projectId ],
        ])

        users[1].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[2], projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : settingValues[3], projectId: projects[2].projectId ],
        ])


        def u1REs_setting1 = users[0].getGlobalMetricsUserSettings(settingNames[0]).sort { it.projectId }
        def u1REs_setting2 = users[0].getGlobalMetricsUserSettings(settingNames[1]).sort { it.projectId }

        def u2REs_setting1 = users[1].getGlobalMetricsUserSettings(settingNames[0]).sort { it.projectId }
        def u2REs_setting2 = users[1].getGlobalMetricsUserSettings(settingNames[1]).sort { it.projectId }
        when:
        // will remove 2 settings
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : null, projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : '', projectId: projects[2].projectId ],
        ])

        def u1REs_setting1_t1 = users[0].getGlobalMetricsUserSettings(settingNames[0]).sort { it.projectId }
        def u1REs_setting2_t1 = users[0].getGlobalMetricsUserSettings(settingNames[1]).sort { it.projectId }

        def u2REs_setting1_t1 = users[1].getGlobalMetricsUserSettings(settingNames[0]).sort { it.projectId }
        def u2REs_setting2_t1 = users[1].getGlobalMetricsUserSettings(settingNames[1]).sort { it.projectId }

        then:
        // user 1
        u1REs_setting1.projectId == [projects[0].projectId, projects[1].projectId, projects[2].projectId]
        u1REs_setting1.quizId == [null, null, null]
        u1REs_setting1.setting == [settingNames[0], settingNames[0], settingNames[0]]
        u1REs_setting1.value == [settingValues[0], settingValues[1], settingValues[2]]

        u1REs_setting2.projectId == [projects[0].projectId]
        u1REs_setting2.quizId == [null]
        u1REs_setting2.setting == [settingNames[1]]
        u1REs_setting2.value == [settingValues[1]]

        // user 2
        u2REs_setting1.projectId == [projects[0].projectId, projects[2].projectId]
        u2REs_setting1.quizId == [null, null]
        u2REs_setting1.setting == [settingNames[0], settingNames[0]]
        u2REs_setting1.value == [settingValues[2], settingValues[3]]

        u2REs_setting2 == []

        // user 1
        u1REs_setting1_t1.projectId == [projects[1].projectId]
        u1REs_setting1_t1.quizId == [null]
        u1REs_setting1_t1.setting == [settingNames[0]]
        u1REs_setting1_t1.value == [settingValues[1]]

        u1REs_setting2_t1.projectId == [projects[0].projectId]
        u1REs_setting2_t1.quizId == [null]
        u1REs_setting2_t1.setting == [settingNames[1]]
        u1REs_setting2_t1.value == [settingValues[1]]

        // user 2
        u2REs_setting1_t1.projectId == [projects[0].projectId, projects[2].projectId]
        u2REs_setting1_t1.quizId == [null, null]
        u2REs_setting1_t1.setting == [settingNames[0], settingNames[0]]
        u2REs_setting1_t1.value == [settingValues[2], settingValues[3]]

        u2REs_setting2_t1 == []
    }

    def "quiz settings with empty value are removed" () {
        List<SkillsService> users = getRandomUsers(3).collect { createService(it)}

        List quizzes = (1..4).collect {
            def quiz = QuizDefFactory.createQuiz(it)
            users[0].createQuizDef(quiz)

            users[0].addQuizUserRole(quiz.quizId, users[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            users[0].addQuizUserRole(quiz.quizId, users[2].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            return quiz
        }

        List<String> settingNames = ["one", "two", "three", "four"]
        List<String> settingValues = ["five", "six", "seven", "eight"]
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[0], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[1], value    : settingValues[1], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : settingValues[1], quizId: quizzes[1].quizId ],
                [ setting  : settingNames[0], value    : settingValues[2], quizId: quizzes[2].quizId ],
        ])

        users[1].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[2], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : settingValues[3], quizId: quizzes[2].quizId ],
        ])


        def u1REs_setting1 = users[0].getGlobalMetricsUserSettings(settingNames[0]).sort { it.quizId }
        def u1REs_setting2 = users[0].getGlobalMetricsUserSettings(settingNames[1]).sort { it.quizId }

        def u2REs_setting1 = users[1].getGlobalMetricsUserSettings(settingNames[0]).sort { it.quizId }
        def u2REs_setting2 = users[1].getGlobalMetricsUserSettings(settingNames[1]).sort { it.quizId }
        when:
        // will remove 2 settings
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : null, quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : '', quizId: quizzes[2].quizId ],
        ])

        def u1REs_setting1_t1 = users[0].getGlobalMetricsUserSettings(settingNames[0]).sort { it.quizId }
        def u1REs_setting2_t1 = users[0].getGlobalMetricsUserSettings(settingNames[1]).sort { it.quizId }

        def u2REs_setting1_t1 = users[1].getGlobalMetricsUserSettings(settingNames[0]).sort { it.quizId }
        def u2REs_setting2_t1 = users[1].getGlobalMetricsUserSettings(settingNames[1]).sort { it.quizId }

        then:
        // user 1
        u1REs_setting1.quizId == [quizzes[0].quizId, quizzes[1].quizId, quizzes[2].quizId]
        u1REs_setting1.projectId == [null, null, null]
        u1REs_setting1.setting == [settingNames[0], settingNames[0], settingNames[0]]
        u1REs_setting1.value == [settingValues[0], settingValues[1], settingValues[2]]

        u1REs_setting2.quizId == [quizzes[0].quizId]
        u1REs_setting2.projectId == [null]
        u1REs_setting2.setting == [settingNames[1]]
        u1REs_setting2.value == [settingValues[1]]

        // user 2
        u2REs_setting1.quizId == [quizzes[0].quizId, quizzes[2].quizId]
        u2REs_setting1.projectId == [null, null]
        u2REs_setting1.setting == [settingNames[0], settingNames[0]]
        u2REs_setting1.value == [settingValues[2], settingValues[3]]

        u2REs_setting2 == []

        // user 1 after removal
        u1REs_setting1_t1.quizId == [quizzes[1].quizId]
        u1REs_setting1_t1.projectId == [null]
        u1REs_setting1_t1.setting == [settingNames[0]]
        u1REs_setting1_t1.value == [settingValues[1]]

        u1REs_setting2_t1.quizId == [quizzes[0].quizId]
        u1REs_setting2_t1.projectId == [null]
        u1REs_setting2_t1.setting == [settingNames[1]]
        u1REs_setting2_t1.value == [settingValues[1]]

        // user 2 (unchanged)
        u2REs_setting1_t1.quizId == [quizzes[0].quizId, quizzes[2].quizId]
        u2REs_setting1_t1.projectId == [null, null]
        u2REs_setting1_t1.setting == [settingNames[0], settingNames[0]]
        u2REs_setting1_t1.value == [settingValues[2], settingValues[3]]

        u2REs_setting2_t1 == []
    }

    def "delete all settings for single user" () {
        List<SkillsService> users = getRandomUsers(2).collect { createService(it)}

        List projects = (1..2).collect {
            def proj = SkillsFactory.createProject(it)
            users[0].createProject(proj)
            users[0].addProjectAdmin(proj.projectId, users[1].userName)
            return proj
        }

        List quizzes = (1..2).collect {
            def quiz = QuizDefFactory.createQuiz(it)
            users[0].createQuizDef(quiz)
            users[0].addQuizUserRole(quiz.quizId, users[1].userName, RoleName.ROLE_QUIZ_ADMIN.toString())
            return quiz
        }

        List<String> settingNames = ["one", "two"]
        List<String> settingValues = ["five", "six"]

        // user 0 - mixed project and quiz settings
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[0], projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : settingValues[1], projectId: projects[1].projectId ],
                [ setting  : settingNames[0], value    : settingValues[0], quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : settingValues[1], quizId: quizzes[1].quizId ],
        ])

        // user 1 - only project settings (unchanged)
        users[1].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : settingValues[1], projectId: projects[0].projectId ],
        ])

        def u1REs_before = users[0].getGlobalMetricsUserSettings(settingNames[0])
        def u2REs_before = users[1].getGlobalMetricsUserSettings(settingNames[0])

        when:
        // delete all settings for user 0 only
        users[0].addOrUpdateGlobalMetricsUserSettings([
                [ setting  : settingNames[0], value    : null, projectId: projects[0].projectId ],
                [ setting  : settingNames[0], value    : null, projectId: projects[1].projectId ],
                [ setting  : settingNames[0], value    : null, quizId: quizzes[0].quizId ],
                [ setting  : settingNames[0], value    : null, quizId: quizzes[1].quizId ],
        ])

        def u1REs_after = users[0].getGlobalMetricsUserSettings(settingNames[0])
        def u2REs_after = users[1].getGlobalMetricsUserSettings(settingNames[0])

        then:
        // verify settings existed before deletion
        u1REs_before.size() == 4
        u2REs_before.size() == 1

        // verify user 0 settings are deleted, user 1 unchanged
        u1REs_after == []
        u2REs_after.size() == 1
    }
}

