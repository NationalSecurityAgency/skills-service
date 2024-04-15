import SkillsDisplay from '@/skills-display/components/SkillsDisplay.vue'
import SkillsDisplayErrorPage from '@/skills-display/components/errors/SkillsDisplayErrorPage.vue'
import MyRankDetailsPage from '@/skills-display/components/rank/MyRankDetailsPage.vue'
import SubjectDetailsPage from '@/skills-display/components/subjects/SubjectPage.vue'

const createSkillsDisplayChildRoutes = (appendToName) => {
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
    name: `MyRankDetailsPage${appendToName}`,
    path: 'rank',
    component: MyRankDetailsPage,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'My Rank'
      }
    }
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
