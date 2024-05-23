import UserPage from '@/components/users/UserPage.vue'
import SkillsDisplayPreview from '@/components/users/SkillsDisplayPreview.vue'
import createSkillsDisplayChildRoutes from '@/router/SkillsDisplayChildRoutes.js'
import PathAppendValues from '@/router/SkillsDisplayPathAppendValues.js'
import UserSkillsPerformed from '@/components/users/UserSkillsPerformed.vue'

const createSkillsDisplayPreviewRoutes = (location, startOfPath) => {
  return {
    path: `${startOfPath}/users/:userId`,
    component: UserPage,
    meta: { requiresAuth: true },
    children: [{
      name: `ClientDisplayPreview`,
      path: '',
      component: SkillsDisplayPreview,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitClientDisplay',
        announcer: {
          message: 'Client Display Preview for user',
        },
      },
      children: createSkillsDisplayChildRoutes(PathAppendValues.SkillsDisplayPreview)
    }, {
      name: `UserSkillEvents${location}`,
      path: 'skillEvents',
      component: UserSkillsPerformed,
      meta: {
        requiresAuth: true,
        reportSkillId: 'VisitUserPerformedSkills',
        announcer: {
          message: 'User\'s Skill Events',
        },
      },
    }],
  }
}

export default createSkillsDisplayPreviewRoutes
