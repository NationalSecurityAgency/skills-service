import SkillsDisplayInIframe from '@/skills-display/SkillsDisplayInIframe.vue'

const createSkillsClientRoutes = (skillsDisplayChildRoutes) => {
  return {
    path: '/static/clientPortal/index.html',
    component: SkillsDisplayInIframe,
    name: 'SkillsDisplayInIframe',
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Skills Display'
      }
    },
    children: skillsDisplayChildRoutes
  }
}

export default createSkillsClientRoutes
