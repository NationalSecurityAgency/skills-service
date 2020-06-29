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
package skills.stressTests

import callStack.profiler.ProfThreadPool
import groovy.util.logging.Slf4j
import skills.intTests.utils.SkillsService

import java.util.concurrent.Callable
import java.util.concurrent.atomic.AtomicLong

@Slf4j
class CreateSkillsDef {

    int numProjects = 10
    int subjPerProject = 6
    int skillsPerSubject = 50
    int badgesPerProject = 10
    boolean remove = false;
    int hasDependenciesEveryNProjects = 3

    List<Proj> create(){
        if(remove) {
            removeProjects()
        }
        return saveProjectsDefs()
    }

    private void removeProjects() {
        List<Proj> projDef = createDef()
        projDef.each {
            String projId = it.id
            SkillsService service = SkillServiceFactory.getService(projId)
            def projects = service.getProjects()
            if (projects?.find({it.projectId == projId})) {
                service.deleteProject(projId)
            }
        }
    }

    static class Proj {
        String id
        String name
        List<Subj> subjs = []
        List<Badge> badges = []
        boolean hasDependencies
    }
    static class Subj {
        String id
        String name
        List<Skill> skills = []
    }
    static class Badge {
        String id
        String name
        List<Skill> skills = []
    }
    static class Skill {
        String id
        String name
        List<Skill> mustCompleteFirst = []
        boolean otherDependOnMe = true
    }

    List<Proj> createDef() {
        List<Proj> res = []
        (1..numProjects).each { int projNum ->
            String projId = getProjectId(projNum)
            Proj proj = new Proj(id: projId, name: "Test Project ${projNum}".toString(), hasDependencies: projNum % hasDependenciesEveryNProjects == 0)
            res << proj
            (1..subjPerProject).each { int subjNum ->
                String subjId = getSubjId(projNum, subjNum)
                Subj subj = new Subj(id: subjId, name: "Test Subject ${subjNum}".toString())
                proj.subjs << subj
                (1..skillsPerSubject).each { int skillNum ->
                    String skillId = getSkillId(projNum, subjNum, skillNum)
                    subj.skills.add(new Skill(id: skillId, name: "${proj.id} - ${subjId} - Test Skill ${skillNum}".toString()))
                }
            }

            List<Skill> allSkills = proj.subjs.collect({it.skills}).flatten()
            int numSkillsPerBadge = (int)(allSkills.size() / badgesPerProject)
            (1..badgesPerProject).each { int badgeNum ->
                List<Skill> badgeSkills = (1..numSkillsPerBadge).collect( { allSkills.remove(0)} )
                Badge badge = new Badge(id: getBadgeId(projNum, badgeNum), name: "Test Subject ${badgeNum}".toString(), skills: badgeSkills)
                proj.badges << badge
            }

            if ( proj.hasDependencies) {
                setupDependencies(proj)
            }
        }


        return res
    }

    private void setupDependencies(Proj proj) {
        List<Skill> depsSkills = proj.subjs.collect({ it.skills }).flatten()
        int numToAdd = 3;
        int currentCounter = 0;
        int currentLevelIndex = 0;
        List<Skill> currentLevel
        List<Skill> nextLevel = []
        while (!depsSkills.isEmpty()) {
            Skill skill = depsSkills.remove(0)
            if (!currentLevel) {
                currentLevel = [skill]
                skill.otherDependOnMe = false
            } else {
                if (currentCounter >= numToAdd) {
                    currentCounter = 0;
                    currentLevel = nextLevel;
                    nextLevel = []
                    numToAdd = numToAdd * 2
                }

                if (currentLevelIndex >= currentLevel.size()) {
                    currentLevelIndex = 0
                }
                currentLevel[currentLevelIndex++].mustCompleteFirst.add(skill)

                nextLevel.add(skill)
                currentCounter++;
            }
        }
    }

    static class RandomLookupKey {
        String projId
        String skillId
    }

    Random r = new Random();

    RandomLookupKey randomLookupKey() {
        int randomProjNum = r.nextInt(numProjects) + 1
        int randomSubjNum = r.nextInt(subjPerProject) + 1
        int randomSkillNum = r.nextInt(skillsPerSubject) + 1
        new RandomLookupKey(
                projId: getProjectId(randomProjNum),
                skillId: getSkillId(randomProjNum, randomSubjNum, randomSkillNum)
        )
    }

    private String getSubjId(Integer projNum, Integer subjNum) {
        "Proj${projNum}Subj${subjNum}".toString()
    }

    private String getBadgeId(Integer projNum, Integer badgeNum) {
        "Proj${projNum}Badge${badgeNum}".toString()
    }

    private String getSkillId(Integer projNum, Integer subjNum, Integer skillNum) {
        "Proj${projNum}Subj${subjNum}Skill${skillNum}".toString()
    }

    private String getProjectId(Integer projNum) {
        "Project${projNum}".toString()
    }

    private List<Proj> saveProjectsDefs(List<Proj> projDefs = null){
        projDefs = projDefs ?: createDef()
        List<Callable> callables = projDefs.collect() { Proj proj ->
            { ->
                saveProject(proj)
                return proj
            } as Callable
        }

        int numThreads = Math.min(5, projDefs.size())
        ProfThreadPool threadPool = new ProfThreadPool("createProjects", numThreads)
        threadPool.warnIfFull = false
        try {
            List<Proj> res = threadPool.asyncExec(callables)
            return res
        } finally {
            threadPool.shutdown()
        }
    }

    AtomicLong counter = new AtomicLong(0)

    private List saveProject(Proj proj) {
        SkillsService service = SkillServiceFactory.getService(proj.id)

        if (!service.projectIdExists([projectId: proj.id])) {
            service.createProject([projectId: proj.id, name: proj.name])
            proj.subjs.each { Subj subj ->
                String subjId = subj.id
                service.createSubject([projectId: proj.id, subjectId: subjId, name: subj.name.toString()])
                subj.skills.each { Skill skill ->
                    service.createSkill([projectId  : proj.id, subjectId: subjId, skillId: skill.id,
                                         name       : skill.name,
                                         type       : "Skill", pointIncrement: 10, numPerformToCompletion: 10, pointIncrementInterval: 8 * 60, numMaxOccurrencesIncrementInterval: 2,
                                         version    : 0,
                                         description: "Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat. Duis aute irure dolor in reprehenderit in voluptate velit esse cillum dolore eu fugiat nulla pariatur. Excepteur sint occaecat cupidatat non proident, sunt in culpa qui officia deserunt mollit anim id est laborum."
                    ])
                    long value = counter.incrementAndGet()
                    if (value > 9 && value % 25 == 0) {
                        log.info("Created [{}] skills. Completed %{}", value, (((double) value / (double) (numProjects * subjPerProject * skillsPerSubject)) * 100).trunc(2))
                    }
                }
            }

            proj.badges.each { Badge badge ->
                service.createBadge([projectId: proj.id, badgeId: badge.id, name: badge.name])
                badge.skills.each {
                    service.assignSkillToBadge([projectId: proj.id, badgeId: badge.id, skillId: it.id])
                }
            }

            if (proj.hasDependencies) {
                Skill rootSkill = proj.subjs.collect({it.skills}).flatten().find({!it.otherDependOnMe})
                addDeps(service, proj, rootSkill)
            }
        }
    }

    private void addDeps(SkillsService service, Proj proj, Skill skill){
        skill.mustCompleteFirst.each {
            service.assignDependency([projectId: proj.id, skillId: skill.id, dependentSkillId: it.id])
            addDeps(service, proj, it)
        }
    }
}
