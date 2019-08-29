package skills.intTests.clientDisplay

import skills.intTests.utils.DefaultIntSpec
import skills.intTests.utils.SkillsFactory

class ClientDisplayRankDistributionSpec extends DefaultIntSpec {

    def "rank distribution - no achieved levels, sad!"() {
        List<String> users = (1..5).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def usr1 = skillsService.getRankDistribution(users.get(0), proj1.projectId)
        then:
        usr1.myPoints == 0
        usr1.myLevel == 0
        usr1.pointsToPassNextUser == -1
        usr1.pointsAnotherUserToPassMe == -1
    }

    def "user's rank - no points"() {
        List<String> users = (1..5).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def usr1Rank = skillsService.getRank(users.get(0), proj1.projectId)
        then:
        usr1Rank.position == 1
        usr1Rank.numUsers == 1
    }

    def "user's rank - only one user"() {
        List<String> users = (1).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], new Date())

        when:
        def usr1Rank = skillsService.getRank(users.get(0), proj1.projectId)
        then:
        usr1Rank.position == 1
        usr1Rank.numUsers == 1
    }

    def "user's rank - two users"() {
        List<String> users = (1..2).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[1], new Date())

        when:
        def usr1Rank = skillsService.getRank(users.get(0), proj1.projectId)
        def usr2Rank = skillsService.getRank(users.get(1), proj1.projectId)
        then:
        usr1Rank.position == 2
        usr1Rank.numUsers == 2

        usr2Rank.position == 1
        usr2Rank.numUsers == 2
    }

    def "ranking distribution - two users"() {
        List<String> users = (1..2).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[1], new Date())

        when:
        def usr1Dist = skillsService.getRankDistribution(users.get(0), proj1.projectId)
        def usr2Dist = skillsService.getRankDistribution(users.get(1), proj1.projectId)
        then:
        usr1Dist.myPoints == 10
        usr1Dist.myLevel == 0
        usr1Dist.pointsToPassNextUser == 10
        usr1Dist.pointsAnotherUserToPassMe == -1

        usr2Dist.myPoints == 20
        usr2Dist.myLevel == 0
        usr2Dist.pointsToPassNextUser == -1
        usr2Dist.pointsAnotherUserToPassMe == 10
    }

    def "ranking distribution - brand new user"() {
        List<String> users = (1..2).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[1], new Date())

        when:
        def dist = skillsService.getRankDistribution("brandNewUser", proj1.projectId)
        then:
        dist.myPoints == 0
        dist.myLevel == 0
        dist.pointsToPassNextUser == 10
        dist.pointsAnotherUserToPassMe == -1
    }

    def "user's rank - two users have the same number of points"() {
        List<String> users = (1..4).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[2], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[2], new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[3], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[3], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], users[3], new Date())

        when:
        def usr1Rank = skillsService.getRank(users.get(0), proj1.projectId)
        def usr2Rank = skillsService.getRank(users.get(1), proj1.projectId)
        def usr3Rank = skillsService.getRank(users.get(2), proj1.projectId)
        def usr4Rank = skillsService.getRank(users.get(3), proj1.projectId)
        then:
        usr1Rank.position == 4
        usr1Rank.numUsers == 4

        usr2Rank.position == 3
        usr2Rank.numUsers == 4

        usr3Rank.position == 3
        usr3Rank.numUsers == 4

        usr4Rank.position == 1
        usr4Rank.numUsers == 4
    }

    def "user's subject rank - two users have the same number of points"() {
        List<String> users = (1..4).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[0], new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[1], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[1], new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[2], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[2], new Date())

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users[3], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(1).skillId], users[3], new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(2).skillId], users[3], new Date())

        when:
        def usr1Rank = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj.subjectId)
        def usr2Rank = skillsService.getRank(users.get(1), proj1.projectId, proj1_subj.subjectId)
        def usr3Rank = skillsService.getRank(users.get(2), proj1.projectId, proj1_subj.subjectId)
        def usr4Rank = skillsService.getRank(users.get(3), proj1.projectId, proj1_subj.subjectId)
        then:
        usr1Rank.position == 4
        usr1Rank.numUsers == 4

        usr2Rank.position == 3
        usr2Rank.numUsers == 4

        usr3Rank.position == 3
        usr3Rank.numUsers == 4

        usr4Rank.position == 1
        usr4Rank.numUsers == 4
    }

    def "users per level - no achieved levels, sad!"() {
        List<String> users = (1..5).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        when:
        def usersPerLevel = skillsService.getUsersPerLevel(proj1.projectId)
        then:
        usersPerLevel == [
                [level: 1, numUsers: 0],
                [level: 2, numUsers: 0],
                [level: 3, numUsers: 0],
                [level: 4, numUsers: 0],
                [level: 5, numUsers: 0]
        ]
    }

    def "rank distribution - some users are in the first level"() {
        List<String> users = (1..5).collect({ "user${it}".toString() })
        List<Date> days = (0..5).collect { new Date() - it }

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(3, 1, 1)
        proj1_skills.each { it.numPerformToCompletion = 10 }

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

//        skillsService.getLevels(proj1.projectId)

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(0), days.get(0))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(1), days.get(1))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(2), days.get(2))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(2))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(3), days.get(3))

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(0))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(1))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(2))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(3))
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(0).skillId], users.get(4), days.get(4))

        when:
        def usersPerLevel = skillsService.getUsersPerLevel(proj1.projectId)

        def usr1 = skillsService.getRankDistribution(users.get(0), proj1.projectId)
        def usr1Rank = skillsService.getRank(users.get(0), proj1.projectId)

        def usr2 = skillsService.getRankDistribution(users.get(1), proj1.projectId)
        def usr2Rank = skillsService.getRank(users.get(1), proj1.projectId)

        def usr3 = skillsService.getRankDistribution(users.get(2), proj1.projectId)
        def usr3Rank = skillsService.getRank(users.get(2), proj1.projectId)

        def usr4 = skillsService.getRankDistribution(users.get(3), proj1.projectId)
        def usr4Rank = skillsService.getRank(users.get(3), proj1.projectId)

        def usr5 = skillsService.getRankDistribution(users.get(4), proj1.projectId)
        def usr5Rank = skillsService.getRank(users.get(4), proj1.projectId)

        then:
        usersPerLevel == [
                [level: 1, numUsers: 3],
                [level: 2, numUsers: 0],
                [level: 3, numUsers: 0],
                [level: 4, numUsers: 0],
                [level: 5, numUsers: 0]
        ]

        usr1.myPoints == 10
        usr1.myLevel == 0
        usr1.pointsToPassNextUser == 10
        usr1.pointsAnotherUserToPassMe == -1
        usr1Rank.position == 5
        usr1Rank.numUsers == 5

        usr2.myPoints == 20
        usr2.myLevel == 0
        usr2.pointsToPassNextUser == 10
        usr2.pointsAnotherUserToPassMe == 10
        usr2Rank.position == 4
        usr2Rank.numUsers == 5

        usr3.myPoints == 30
        usr3.myLevel == 1
        usr3.pointsToPassNextUser == 10
        usr3.pointsAnotherUserToPassMe == 10
        usr3Rank.position == 3
        usr3Rank.numUsers == 5

        usr4.myPoints == 40
        usr4.myLevel == 1
        usr4.pointsToPassNextUser == 10
        usr4.pointsAnotherUserToPassMe == 10
        usr4Rank.position == 2
        usr4Rank.numUsers == 5

        usr5.myPoints == 50
        usr5.myLevel == 1
        usr5.pointsToPassNextUser == -1
        usr5.pointsAnotherUserToPassMe == 10
        usr5Rank.position == 1
        usr5Rank.numUsers == 5
    }

    def "rank distribution - users in all levels"() {
        List<String> users = (1..20).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_skills = SkillsFactory.createSkills(21, 1, 1)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj)
        skillsService.createSkills(proj1_skills)

        skillsService.getLevels(proj1.projectId)


        // level 1 - 4 users
        (0..2).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(0), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(1), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(2), new Date())
        }
        (0..3).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(3), new Date())
        }

        // level 2 - 3 users
        (0..5).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(4), new Date())
        }
        (0..6).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(5), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(6), new Date())
        }

        // level 3 - 2 users
        (0..9).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(7), new Date())
        }
        (0..10).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(8), new Date())
        }

        // level 4 - 5 users
        (0..14).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(9), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(10), new Date())
        }
        (0..15).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(11), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(12), new Date())
        }

        // level 5 - 2 users
        (0..19).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(13), new Date())
        }
        (0..20).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_skills.get(it).skillId], users.get(14), new Date())
        }

        when:
        def usersPerLevel = skillsService.getUsersPerLevel(proj1.projectId)
        List usrRes = users.collect {
            [
                    rankDist: skillsService.getRankDistribution(it, proj1.projectId),
                    rank: skillsService.getRank(it, proj1.projectId)
            ]
        }
        then:
        usersPerLevel == [
                [level: 1, numUsers: 4],
                [level: 2, numUsers: 3],
                [level: 3, numUsers: 2],
                [level: 4, numUsers: 4],
                [level: 5, numUsers: 2]
        ]
        usrRes.each {
            it.rank.numUsers == 15
        }
        usrRes.rank.get(0).position == 15
        usrRes.rankDist.get(0).myPoints == 30
        usrRes.rankDist.get(0).myLevel == 1
        usrRes.rankDist.get(0).pointsAnotherUserToPassMe == -1
        usrRes.rankDist.get(0).pointsToPassNextUser == 10

        usrRes.rank.get(1).position == 15
        usrRes.rankDist.get(1).myPoints == 30
        usrRes.rankDist.get(1).myLevel == 1
        usrRes.rankDist.get(1).pointsAnotherUserToPassMe == -1
        usrRes.rankDist.get(1).pointsToPassNextUser == 10

        usrRes.rank.get(2).position == 15
        usrRes.rankDist.get(2).myPoints == 30
        usrRes.rankDist.get(2).myLevel == 1
        usrRes.rankDist.get(2).pointsAnotherUserToPassMe == -1
        usrRes.rankDist.get(2).pointsToPassNextUser == 10

        usrRes.rank.get(3).position == 12
        usrRes.rankDist.get(3).myPoints == 40
        usrRes.rankDist.get(3).myLevel == 1
        usrRes.rankDist.get(3).pointsAnotherUserToPassMe == 10
        usrRes.rankDist.get(3).pointsToPassNextUser == 20

        usrRes.rank.get(4).position == 11
        usrRes.rankDist.get(4).myPoints == 60
        usrRes.rankDist.get(4).myLevel == 2
        usrRes.rankDist.get(4).pointsAnotherUserToPassMe == 20
        usrRes.rankDist.get(4).pointsToPassNextUser == 10

        usrRes.rank.get(5).position == 10
        usrRes.rankDist.get(5).myPoints == 70
        usrRes.rankDist.get(5).myLevel == 2
        usrRes.rankDist.get(5).pointsAnotherUserToPassMe == 10
        usrRes.rankDist.get(5).pointsToPassNextUser == 30

        usrRes.rank.get(6).position == 10
        usrRes.rankDist.get(6).myPoints == 70
        usrRes.rankDist.get(6).myLevel == 2
        usrRes.rankDist.get(6).pointsAnotherUserToPassMe == 10
        usrRes.rankDist.get(6).pointsToPassNextUser == 30

        usrRes.rank.get(7).position == 8
        usrRes.rankDist.get(7).myPoints == 100
        usrRes.rankDist.get(7).myLevel == 3
        usrRes.rankDist.get(7).pointsAnotherUserToPassMe == 30
        usrRes.rankDist.get(7).pointsToPassNextUser == 10

        usrRes.rank.get(8).position == 7
        usrRes.rankDist.get(8).myPoints == 110
        usrRes.rankDist.get(8).myLevel == 3
        usrRes.rankDist.get(8).pointsAnotherUserToPassMe == 10
        usrRes.rankDist.get(8).pointsToPassNextUser == 40

        usrRes.rank.get(9).position == 6
        usrRes.rankDist.get(9).myPoints == 150
        usrRes.rankDist.get(9).myLevel == 4
        usrRes.rankDist.get(9).pointsAnotherUserToPassMe == 40
        usrRes.rankDist.get(9).pointsToPassNextUser == 10

        usrRes.rank.get(10).position == 6
        usrRes.rankDist.get(10).myPoints == 150
        usrRes.rankDist.get(10).myLevel == 4
        usrRes.rankDist.get(10).pointsAnotherUserToPassMe == 40
        usrRes.rankDist.get(10).pointsToPassNextUser == 10

        usrRes.rank.get(11).position == 4
        usrRes.rankDist.get(11).myPoints == 160
        usrRes.rankDist.get(11).myLevel == 4
        usrRes.rankDist.get(11).pointsAnotherUserToPassMe == 10
        usrRes.rankDist.get(11).pointsToPassNextUser == 40

        usrRes.rank.get(12).position == 4
        usrRes.rankDist.get(12).myPoints == 160
        usrRes.rankDist.get(12).myLevel == 4
        usrRes.rankDist.get(12).pointsAnotherUserToPassMe == 10
        usrRes.rankDist.get(12).pointsToPassNextUser == 40

        usrRes.rank.get(13).position == 2
        usrRes.rankDist.get(13).myPoints == 200
        usrRes.rankDist.get(13).myLevel == 5
        usrRes.rankDist.get(13).pointsAnotherUserToPassMe == 40
        usrRes.rankDist.get(13).pointsToPassNextUser == 10

        usrRes.rank.get(14).position == 1
        usrRes.rankDist.get(14).myPoints == 210
        usrRes.rankDist.get(14).myLevel == 5
        usrRes.rankDist.get(14).pointsAnotherUserToPassMe == 10
        usrRes.rankDist.get(14).pointsToPassNextUser == -1
    }

    def "rank distribution - multiple subjects"() {
        List<String> users = (1..10).collect({ "user${it}".toString() })

        def proj1 = SkillsFactory.createProject(1)
        def proj1_subj1 = SkillsFactory.createSubject(1, 1)
        List<Map> proj1_subj1_skills = SkillsFactory.createSkills(10, 1, 1)

        def proj1_subj2 = SkillsFactory.createSubject(1, 2)
        List<Map> proj1_subj2_skills = SkillsFactory.createSkills(20, 1, 2)

        def proj1_subj3 = SkillsFactory.createSubject(1, 3)
        List<Map> proj1_subj3_skills = SkillsFactory.createSkills(20, 1, 3)

        skillsService.createProject(proj1)
        skillsService.createSubject(proj1_subj1)
        skillsService.createSubject(proj1_subj2)
        skillsService.createSubject(proj1_subj3)
        skillsService.createSkills(proj1_subj1_skills)
        skillsService.createSkills(proj1_subj2_skills)
        skillsService.createSkills(proj1_subj3_skills)

        when:
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj1_skills.get(0).skillId], users.get(0), new Date())
        (0..4).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(it).skillId], users.get(0), new Date())
        }
        (0..13).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj3_skills.get(it).skillId], users.get(0), new Date())
        }
        def usr1Subj1 = skillsService.getRankDistribution(users.get(0), proj1.projectId, proj1_subj1.subjectId)
        def usr1Subj2 = skillsService.getRankDistribution(users.get(0), proj1.projectId, proj1_subj2.subjectId)
        def usr1Subj3 = skillsService.getRankDistribution(users.get(0), proj1.projectId, proj1_subj3.subjectId)
        def usersPerLevelSubj1 = skillsService.getUsersPerLevel(proj1.projectId, proj1_subj1.subjectId)
        def usersPerLevelSubj2 = skillsService.getUsersPerLevel(proj1.projectId, proj1_subj2.subjectId)
        def usersPerLevelSubj3 = skillsService.getUsersPerLevel(proj1.projectId, proj1_subj3.subjectId)
        def rankSubj1 = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj1.subjectId)
        def rankSubj2 = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj3.subjectId)
        def rankSubj3 = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj1.subjectId)

        (0..4).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(it).skillId], users.get(1), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(it).skillId], users.get(2), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(it).skillId], users.get(3), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(it).skillId], users.get(4), new Date())
        }

        (0..10).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(it).skillId], users.get(5), new Date())
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(it).skillId], users.get(6), new Date())
        }

        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(0).skillId], users.get(4), new Date())
        skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj2_skills.get(5).skillId], users.get(4), new Date())

        (0..13).each {
            skillsService.addSkill([projectId: proj1.projectId, skillId: proj1_subj3_skills.get(it).skillId], users.get(0), new Date())
        }

        def usersPerLevelSubj1_res1 = skillsService.getUsersPerLevel(proj1.projectId, proj1_subj1.subjectId)
        def usersPerLevelSubj2_res1 = skillsService.getUsersPerLevel(proj1.projectId, proj1_subj2.subjectId)
        def usersPerLevelSubj3_res1 = skillsService.getUsersPerLevel(proj1.projectId, proj1_subj3.subjectId)
        def rankSubj1_res1 = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj1.subjectId)
        def rankSubj2_res1 = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj2.subjectId)
        def rankSubj3_res1 = skillsService.getRank(users.get(0), proj1.projectId, proj1_subj3.subjectId)

        then:
        rankSubj1.position == 1
        rankSubj1.numUsers == 1
        usr1Subj1.myPoints == 10
        usr1Subj1.myLevel == 1
        usr1Subj1.pointsToPassNextUser == -1
        usr1Subj1.pointsAnotherUserToPassMe == -1
        usersPerLevelSubj1 == [
                [level: 1, numUsers: 1],
                [level: 2, numUsers: 0],
                [level: 3, numUsers: 0],
                [level: 4, numUsers: 0],
                [level: 5, numUsers: 0]
        ]

        usr1Subj2.myPoints == 50
        usr1Subj2.myLevel == 2
        usr1Subj2.pointsToPassNextUser == -1
        usr1Subj2.pointsAnotherUserToPassMe == -1
        rankSubj2.position == 1
        rankSubj2.numUsers == 1
        usersPerLevelSubj2 == [
                [level: 1, numUsers: 0],
                [level: 2, numUsers: 1],
                [level: 3, numUsers: 0],
                [level: 4, numUsers: 0],
                [level: 5, numUsers: 0]
        ]

        usr1Subj3.myPoints == 140
        usr1Subj3.myLevel == 4
        usr1Subj3.pointsToPassNextUser == -1
        usr1Subj3.pointsAnotherUserToPassMe == -1
        rankSubj3.position == 1
        rankSubj3.numUsers == 1
        usersPerLevelSubj3 == [
                [level: 1, numUsers: 0],
                [level: 2, numUsers: 0],
                [level: 3, numUsers: 0],
                [level: 4, numUsers: 1],
                [level: 5, numUsers: 0]
        ]

        // subject 1 was not affected
        rankSubj1_res1.position == 1
        rankSubj1_res1.numUsers == 1
        usersPerLevelSubj1_res1 == [
                [level: 1, numUsers: 1],
                [level: 2, numUsers: 0],
                [level: 3, numUsers: 0],
                [level: 4, numUsers: 0],
                [level: 5, numUsers: 0]
        ]

        rankSubj2_res1.position == 7
        rankSubj2_res1.numUsers == 7
        usersPerLevelSubj2_res1 == [
                [level: 1, numUsers: 0],
                [level: 2, numUsers: 5],
                [level: 3, numUsers: 2],
                [level: 4, numUsers: 0],
                [level: 5, numUsers: 0]
        ]

        // subject 3 was not affected
        rankSubj3_res1.position == 1
        rankSubj3_res1.numUsers == 1
        usersPerLevelSubj3_res1 == [
                [level: 1, numUsers: 0],
                [level: 2, numUsers: 0],
                [level: 3, numUsers: 0],
                [level: 4, numUsers: 1],
                [level: 5, numUsers: 0]
        ]
    }

}
