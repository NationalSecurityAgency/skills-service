/*
 * Copyright 2024 SkillTree
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
import SkillsDisplay from '@/skills-display/components/SkillsDisplay.vue'
import SkillsDisplayErrorPage from '@/skills-display/components/errors/SkillsDisplayErrorPage.vue'
import MyRankDetailsPage from '@/skills-display/components/rank/MyRankDetailsPage.vue'
import SubjectDetailsPage from '@/skills-display/components/subjects/SubjectPage.vue'
import SkillPage from '@/skills-display/components/skill/SkillPage.vue'
import BadgesDetailsPage from '@/skills-display/components/badges/BadgesDetailsPage.vue'
import BadgeDetailsPage from '@/skills-display/components/badges/BadgeDetailsPage.vue'
import QuizPage from '@/skills-display/components/quiz/QuizPage.vue'

const createSkillsDisplayChildRoutes = (appendToName, startWithSlash = false) => {

  const projectPlaceholder = '##PROJECT##'
  const projectPlaceholderRegex = new RegExp(projectPlaceholder, 'g')
  const subjectPlaceholder = '##SUBJECT##'
  const subjectPlaceholderRegex = new RegExp(subjectPlaceholder, 'g')
  const groupPlaceholder = '##GROUP##'
  const groupPlaceholderRegex = new RegExp(groupPlaceholder, 'g')
  const skillPlaceholder = '##SKILL##'
  const skillPlaceholderRegex = new RegExp(skillPlaceholder, 'g')

  const prependToPath = startWithSlash ? '/' : ''
  return [{
    name: `SkillsDisplay${appendToName}`,
    path: `${prependToPath}`,
    component: SkillsDisplay,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Skills Display'
      }
    }
  }, {
    path: `${prependToPath}rank`,
    component: MyRankDetailsPage,
    name: `myRankDetails${appendToName}`,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'My Rank'
      }
    }
  }, {
    path: `${prependToPath}subjects/:subjectId/rank`,
    component: MyRankDetailsPage,
    name: `subjectRankDetails${appendToName}`,
    props: true,
    meta: {
      title: `My ${subjectPlaceholder} Rank`
    }
  }, {
    name: `BadgesDetailsPage${appendToName}`,
    path: `${prependToPath}badges`,
    component: BadgesDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Badges'
      }
    }
  }, {
    path: `${prependToPath}badges/:badgeId`,
    component: BadgeDetailsPage,
    name: `badgeDetails${appendToName}`,
    props: true,
    meta: {
      title: 'Badge Details'
    }
  }, {
    path: `${prependToPath}badges/global/:badgeId`,
    component: BadgeDetailsPage,
    name: `globalBadgeDetails${appendToName}`,
    props: true,
    meta: {
      title: 'Global Badge Details'
    }
  }, {
    path: `${prependToPath}badges/global/:badgeId/skills/:skillId`,
    component: SkillPage,
    name: `globalBadgeSkillDetails${appendToName}`,
    meta: {
      title: `Global Badge ${skillPlaceholder} Details`
    }
  }, {
    path: `${prependToPath}badges/global/:badgeId/crossProject/:crossProjectId/:dependentSkillId`,
    component: SkillPage,
    name: `globalBadgeSkillDetailsUnderAnotherProject${appendToName}`,
    meta: {
      title: `Global Badge ${skillPlaceholder} Details`
    }
  }, {
    path: `${prependToPath}badges/:badgeId/skills/:skillId`,
    component: SkillPage,
    name: `badgeSkillDetails${appendToName}`,
    meta: {
      title: `Badge ${skillPlaceholder} Details`
    }
  }, {
    path: `${prependToPath}badges/:badgeId/crossProject/:crossProjectId/:dependentSkillId`,
    component: SkillPage,
    name: `crossProjectSkillDetailsUnderBadge${appendToName}`,
    meta: {
      title: `Cross ${projectPlaceholder} ${skillPlaceholder} Details`
    }
  }, {
    name: `SubjectDetailsPage${appendToName}`,
    path: `${prependToPath}subjects/:subjectId`,
    component: SubjectDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Subject'
      }
    }
  }, {
    path: `${prependToPath}subjects/:subjectId/skills/:skillId`,
    component: SkillPage,
    name: `skillDetails${appendToName}`,
    meta: {
      title: `${skillPlaceholder} Details`
    }
  }, {
    path: `${prependToPath}subjects/:subjectId/skills/:skillId/crossProject/:crossProjectId/:dependentSkillId`,
    component: SkillPage,
    name: `crossProjectSkillDetails${appendToName}`,
    meta: {
      title: `Cross ${projectPlaceholder} ${skillPlaceholder} Details`
    }
  }, {
    name: `quizPage${appendToName}`,
    path: `${prependToPath}subjects/:subjectId/skills/:skillId/quizzes/:quizId`,
    component: QuizPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Quiz or Survey Run'
      }
    }
  }, {
    name: `quizPageForGlobalBadgeCrossProjectSkill${appendToName}`,
    path: `${prependToPath}badges/global/:badgeId/crossProject/:crossProjectId/:skillId/quizzes/:quizId`,
    component: QuizPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Quiz or Survey Run'
      }
    }
  }, {
    name: `SkillsDisplayErrorPage${appendToName}`,
    path: `${prependToPath}error`,
    component: SkillsDisplayErrorPage,
    meta: {
      requiresAuth: false,
      nonAdmin: true,
      announcer: {
        message: 'Error Page'
      }
    }
  }]
}

export default createSkillsDisplayChildRoutes
