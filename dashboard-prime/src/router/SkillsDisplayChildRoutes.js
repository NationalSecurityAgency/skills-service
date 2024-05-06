import SkillsDisplay from '@/skills-display/components/SkillsDisplay.vue'
import SkillsDisplayErrorPage from '@/skills-display/components/errors/SkillsDisplayErrorPage.vue'
import MyRankDetailsPage from '@/skills-display/components/rank/MyRankDetailsPage.vue'
import SubjectDetailsPage from '@/skills-display/components/subjects/SubjectPage.vue'
import SkillPage from '@/skills-display/components/skill/SkillPage.vue'
import BadgesDetailsPage from '@/skills-display/components/badges/BadgesDetailsPage.vue'
import BadgeDetailsPage from '@/skills-display/components/badges/BadgeDetailsPage.vue'
import QuizPage from '@/skills-display/components/quiz/QuizPage.vue'

const createSkillsDisplayChildRoutes = (appendToName) => {

  const projectPlaceholder = '##PROJECT##'
  const projectPlaceholderRegex = new RegExp(projectPlaceholder, 'g')
  const subjectPlaceholder = '##SUBJECT##'
  const subjectPlaceholderRegex = new RegExp(subjectPlaceholder, 'g')
  const groupPlaceholder = '##GROUP##'
  const groupPlaceholderRegex = new RegExp(groupPlaceholder, 'g')
  const skillPlaceholder = '##SKILL##'
  const skillPlaceholderRegex = new RegExp(skillPlaceholder, 'g')


  return [{
    name: `SkillsDisplay${appendToName}`,
    path: '',
    component: SkillsDisplay,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Skills Display'
      }
    }
  }, {
    path: 'rank',
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
    path: 'subjects/:subjectId/rank',
    component: MyRankDetailsPage,
    name: `subjectRankDetails${appendToName}`,
    props: true,
    meta: {
      title: `My ${subjectPlaceholder} Rank`,
    },
  }, {
    name: `BadgesDetailsPage${appendToName}`,
    path: 'badges',
    component: BadgesDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Badges'
      }
    }
  }, {
    path: 'badges/:badgeId',
    component: BadgeDetailsPage,
    name: `badgeDetails${appendToName}`,
    props: true,
    meta: {
      title: 'Badge Details'
    }
  }, {
    path: 'badges/global/:badgeId',
    component: BadgeDetailsPage,
    name: `globalBadgeDetails${appendToName}`,
    props: true,
    meta: {
      title: 'Global Badge Details'
    }
  }, {
    path: 'badges/global/:badgeId/skills/:skillId',
    component: SkillPage,
    name: `globalBadgeSkillDetails${appendToName}`,
    meta: {
      title: `Global Badge ${skillPlaceholder} Details`,
    },
  }, {
    path: 'badges/:badgeId/skills/:skillId',
    component: SkillPage,
    name: `badgeSkillDetails${appendToName}`,
    meta: {
      title: `Badge ${skillPlaceholder} Details`,
    },
  }, {
    name: `SubjectDetailsPage${appendToName}`,
    path: 'subjects/:subjectId',
    component: SubjectDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Subject'
      }
    }
  }, {
    path: 'subjects/:subjectId/skills/:skillId',
    component: SkillPage,
    name: `skillDetails${appendToName}`,
    meta: {
      title: `${skillPlaceholder} Details`
    }
  }, {
    path: 'subjects/:subjectId/skills/:skillId/crossProject/:crossProjectId/:dependentSkillId',
    component: SkillPage,
    name: `crossProjectSkillDetails${appendToName}`,
    meta: {
      title: `Cross ${projectPlaceholder} ${skillPlaceholder} Details`,
    },
  },{
    name: `quizPage${appendToName}`,
    path: 'subjects/:subjectId/skills/:skillId/quizzes/:quizId',
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
    path: 'error',
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
