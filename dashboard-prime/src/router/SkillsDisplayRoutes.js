import SkillsDisplay from '@/skills-display/components/SkillsDisplay.vue'
import SkillsDisplayErrorPage from '@/skills-display/components/errors/SkillsDisplayErrorPage.vue'
import SkillsDisplayInIframe from '@/skills-display/SkillsDisplayInIframe.vue'
import MyRankDetailsPage from '@/skills-display/components/rank/MyRankDetailsPage.vue'
import SubjectDetailsPage from '@/skills-display/components/subjects/SubjectPage.vue'

const createSkillsDisplayRoutes = () => {
  return {
    path: '/static/clientPortal/index.html',
    component: SkillsDisplayInIframe,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Skills Display'
      }
    },
    children: [{
      name: 'SkillsDisplay',
      path: '',
      component: SkillsDisplay,
      meta: {
        requiresAuth: true,
        nonAdmin: true,
        announcer: {
          message: 'Skills Display',
        },
      },
    }, {
      name: 'MyRankDetailsPage',
      path: 'rank',
      component: MyRankDetailsPage,
      meta: {
        requiresAuth: true,
        nonAdmin: true,
        announcer: {
          message: 'My Rank',
        },
      },
    }, {
      name: 'SubjectDetailsPage',
      path: 'subjects/:subjectId',
      component: SubjectDetailsPage,
      meta: {
        requiresAuth: true,
        nonAdmin: true,
        announcer: {
          message: 'Subject',
        },
      },
    }, {
      name: 'SkillsDisplayErrorPage',
      path: 'error',
      component: SkillsDisplayErrorPage,
      meta: {
        requiresAuth: false,
        nonAdmin: true,
        announcer: {
          message: 'Error Page',
        },
      },
    }]
  }
}

export default createSkillsDisplayRoutes
