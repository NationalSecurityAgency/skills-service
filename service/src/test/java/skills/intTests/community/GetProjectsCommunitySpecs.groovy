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
package skills.intTests.community


import org.springframework.beans.factory.annotation.Autowired
import org.springframework.transaction.PlatformTransactionManager
import org.springframework.transaction.support.TransactionTemplate
import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory
import skills.intTests.utils.SkillsService
import skills.storage.model.auth.RoleName
import skills.storage.repos.UserTagRepo

import static skills.intTests.utils.SkillsFactory.createProject

class GetProjectsCommunitySpecs extends DefaultIntSpec {

    @Autowired
    private PlatformTransactionManager transactionManager

    @Autowired
    UserTagRepo userTagRepo

    def "get single project - community info is only returned for community members"() {
        List<String> users = getRandomUsers(2)

        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])

        def p1 = createProject(1)
        p1.enableProtectedUserCommunity = true
        pristineDragonsUser.createProject(p1)

        def p2 = createProject(2)
        allDragonsUser.createProject(p2)

        allDragonsUser.addUserRole(pristineDragonsUser.userName, p2.projectId, RoleName.ROLE_PROJECT_ADMIN.toString())

        when:
        def pristineDragonUserP1 = pristineDragonsUser.getProject(p1.projectId)
        def pristineDragonUserP2 = pristineDragonsUser.getProject(p2.projectId)
        def allDragonsUserP2 = allDragonsUser.getProject(p2.projectId)
        then:
        pristineDragonUserP1.projectId == p1.projectId
        pristineDragonUserP1.userCommunity == 'Divine Dragon'

        pristineDragonUserP2.projectId == p2.projectId
        pristineDragonUserP2.userCommunity == 'All Dragons'

        allDragonsUserP2.projectId == p2.projectId
        allDragonsUserP2.userCommunity == null
    }

    def "user community project is not included in availableForMyProjects when user is not a member of the UC"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..3).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 3 // 3rd project has user community protection enabled
            pristineDragonsUser.createProject(proj)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }
        // 2nd project is NOT in the production mode, 3rd project has user community protection enabled
        pristineDragonsUser.enableProdMode(projs[0])
        pristineDragonsUser.enableProdMode(projs[2])

        when:
        def forMyProjects = allDragonsUser.getAvailableMyProjects()

        then:
        forMyProjects.size() == 1

        forMyProjects[0].projectId == projs[0].projectId
        forMyProjects[0].name == projs[0].name
        forMyProjects[0].totalPoints == 10
        forMyProjects[0].numSubjects == 1
        forMyProjects[0].numSkills == 1
        forMyProjects[0].numBadges == 3
        !forMyProjects[0].isMyProject
    }

    def "user community project is included in availableForMyProjects when user is a member of the UC"() {
        List<String> users = getRandomUsers(2)
        SkillsService otherPristineDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(otherPristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..3).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 3 // 3rd project has user community protection enabled
            pristineDragonsUser.createProject(proj)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }
        // 2nd project is NOT in the production mode
        pristineDragonsUser.enableProdMode(projs[0])
        pristineDragonsUser.enableProdMode(projs[2])

        when:
        def forMyProjects = otherPristineDragonsUser.getAvailableMyProjects()

        then:
        forMyProjects.size() == 2

        forMyProjects[0].projectId == projs[0].projectId
        forMyProjects[0].name == projs[0].name
        forMyProjects[0].totalPoints == 10
        forMyProjects[0].numSubjects == 1
        forMyProjects[0].numSkills == 1
        forMyProjects[0].numBadges == 3
        !forMyProjects[0].isMyProject

        forMyProjects[1].projectId == projs[2].projectId
        forMyProjects[1].name == projs[2].name
        forMyProjects[1].totalPoints == 30
        forMyProjects[1].numSubjects == 3
        forMyProjects[1].numSkills == 3
        forMyProjects[1].numBadges == 1
        !forMyProjects[1].isMyProject
    }

    def "user community project is not included in availableForMyProjects when root is not a member of the UC"() {
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..3).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 3 // 3rd project has user community protection enabled
            pristineDragonsUser.createProject(proj)
            rootUser.pinProject(proj.projectId)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }
        // 2nd project is NOT in the production mode, 3rd project has user community protection enabled
        pristineDragonsUser.enableProdMode(projs[0])
        pristineDragonsUser.enableProdMode(projs[2])

        when:
        def rootForMyProjects = rootUser.getAvailableMyProjects()

        then:

        rootForMyProjects.size() == 1
        rootForMyProjects[0].projectId == projs[0].projectId
        rootForMyProjects[0].name == projs[0].name
        rootForMyProjects[0].totalPoints == 10
        rootForMyProjects[0].numSubjects == 1
        rootForMyProjects[0].numSkills == 1
        rootForMyProjects[0].numBadges == 3
        !rootForMyProjects[0].isMyProject
    }

    def "user community project is included in availableForMyProjects when root is a member of the UC"() {
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..3).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 3 // 3rd project has user community protection enabled
            pristineDragonsUser.createProject(proj)
            rootUser.pinProject(proj.projectId)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }
        // 2nd project is NOT in the production mode
        pristineDragonsUser.enableProdMode(projs[0])
        pristineDragonsUser.enableProdMode(projs[2])

        when:
        def rootForMyProjects = rootUser.getAvailableMyProjects()

        then:

        rootForMyProjects.size() == 2
        rootForMyProjects[0].projectId == projs[0].projectId
        rootForMyProjects[0].name == projs[0].name
        rootForMyProjects[0].totalPoints == 10
        rootForMyProjects[0].numSubjects == 1
        rootForMyProjects[0].numSkills == 1
        rootForMyProjects[0].numBadges == 3
        !rootForMyProjects[0].isMyProject

        rootForMyProjects[1].projectId == projs[2].projectId
        rootForMyProjects[1].name == projs[2].name
        rootForMyProjects[1].totalPoints == 30
        rootForMyProjects[1].numSubjects == 3
        rootForMyProjects[1].numSkills == 3
        rootForMyProjects[1].numBadges == 1
        !rootForMyProjects[1].isMyProject
    }

    def "user community project is not included in admin projects when admin is not a member of the UC"() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..2).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 2 // 2nd project has user community protection enabled
            pristineDragonsUser.createProject(proj)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }

        transactionTemplate.execute({
            userTagRepo.deleteByUserId(pristineDragonsUser.userName)
        })

        when:
        def adminProjects = pristineDragonsUser.getProjects()

        then:
        adminProjects.size() == 1
        adminProjects[0].projectId == projs[0].projectId
        adminProjects[0].name == projs[0].name
    }

    def "user community project is included in admin projects when admin is a member of the UC"() {
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..2).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 2 // 2nd project has user community protection enabled
            pristineDragonsUser.createProject(proj)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }

        when:
        def adminProjects = pristineDragonsUser.getProjects()

        then:
        adminProjects.size() == 2
        adminProjects[0].projectId == projs[0].projectId
        adminProjects[0].name == projs[0].name

        adminProjects[1].projectId == projs[1].projectId
        adminProjects[1].name == projs[1].name
    }

    def "user community project is not included in admin projects when root is not a member of the UC"() {
        TransactionTemplate transactionTemplate = new TransactionTemplate(transactionManager)
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..2).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 2 // 2nd project has user community protection enabled
            pristineDragonsUser.createProject(proj)
            rootUser.pinProject(proj.projectId)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }

        transactionTemplate.execute({
            userTagRepo.deleteByUserId(pristineDragonsUser.userName)
        })

        when:
        def adminProjects = rootUser.getProjects()

        then:

        adminProjects.size() == 1
        adminProjects[0].projectId == projs[0].projectId
        adminProjects[0].name == projs[0].name
    }

    def "user community project is included in admin projects when root is a member of the UC"() {
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..2).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            proj.enableProtectedUserCommunity = numProj == 2 // 2nd project has user community protection enabled
            pristineDragonsUser.createProject(proj)
            rootUser.pinProject(proj.projectId)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            return proj
        }

        when:
        def adminProjects = rootUser.getProjects()

        then:
        adminProjects.size() == 2
        adminProjects[0].projectId == projs[0].projectId
        adminProjects[0].name == projs[0].name

        adminProjects[1].projectId == projs[1].projectId
        adminProjects[1].name == projs[1].name
    }

    def "user community project is included in my progress summary when user is a member of the UC"() {
        SkillsService pristineDragonsUser = createService(getRandomUsers(1))
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        rootUser.saveUserTag(rootUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..2).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            pristineDragonsUser.createProject(proj)
            pristineDragonsUser.enableProdMode(proj)
            pristineDragonsUser.addMyProject(proj.projectId)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
                pristineDragonsUser.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
                badge.enabled = true
                pristineDragonsUser.updateBadge(badge)
            }

            if (numProj == 2) {
                proj.enableProtectedUserCommunity = true // 2nd project has user community protection enabled
                pristineDragonsUser.updateProject(proj)
            }
            return proj
        }

        when:
        def res = pristineDragonsUser.getMyProgressSummary()
        def chartData = pristineDragonsUser.getApiGlobalMetricsData("allProjectsSkillEventsOverTimeMetricsBuilder", [start: (new Date() - 14).time, projIds: "${projs[0].projectId},${projs[1].projectId}"])
        def myBadges = pristineDragonsUser.getMyProgressBadges()

        then:
        res.projectSummaries.size() == 2
        res.projectSummaries.find { it.projectId == projs[0].projectId }
        res.projectSummaries.find { it.projectId == projs[1].projectId }

        chartData.find {it.project ==  projs[0].projectId}
        chartData.find {it.project ==  projs[1].projectId}

        myBadges.find {it.projectId ==  projs[0].projectId}
        myBadges.find {it.projectId ==  projs[1].projectId}
    }

    def "user community project is not included in my progress summary when user is NOT a member of the UC"() {
        List<String> users = getRandomUsers(2)
        SkillsService allDragonsUser = createService(users[0])
        SkillsService pristineDragonsUser = createService(users[1])
        SkillsService rootUser = createRootSkillService()
        rootUser.saveUserTag(pristineDragonsUser.userName, 'dragons', ['DivineDragon'])
        List projs = (1..2).collect { int numProj ->
            def proj = SkillsFactory.createProject(numProj)
            pristineDragonsUser.createProject(proj)
            pristineDragonsUser.enableProdMode(proj)
            allDragonsUser.addMyProject(proj.projectId)

            (1..numProj).each {
                def subj = SkillsFactory.createSubject(numProj, it)
                pristineDragonsUser.createSubject(subj)
            }
            def skills = SkillsFactory.createSkills(numProj, numProj, 1)
            pristineDragonsUser.createSkills(skills)
            (1..(4-numProj)).each {
                def badge = SkillsFactory.createBadge(numProj, it)
                pristineDragonsUser.createBadge(badge)
                pristineDragonsUser.assignSkillToBadge([projectId: proj.projectId, badgeId: badge.badgeId, skillId: skills[0].skillId])
                badge.enabled = true
                pristineDragonsUser.updateBadge(badge)
            }

            if (numProj == 2) {
                proj.enableProtectedUserCommunity = true // 2nd project has user community protection enabled
                pristineDragonsUser.updateProject(proj)
            }
            return proj
        }

        when:
        def res = allDragonsUser.getMyProgressSummary()
        def chartData = allDragonsUser.getApiGlobalMetricsData("allProjectsSkillEventsOverTimeMetricsBuilder", [start: (new Date() - 14).time, projIds: "${projs[0].projectId},${projs[1].projectId}"])
        def myBadges = allDragonsUser.getMyProgressBadges()

        then:
        res.projectSummaries.size() == 1
        res.projectSummaries.find { it.projectId == projs[0].projectId }

        chartData.find {it.project ==  projs[0].projectId}
        !chartData.find {it.project ==  projs[1].projectId}

        myBadges.find {it.projectId ==  projs[0].projectId}
        !myBadges.find {it.projectId ==  projs[1].projectId}
    }

}
