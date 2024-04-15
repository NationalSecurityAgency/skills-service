import MyProjectSkillsPage from '@/components/myProgress/MyProjectSkillsPage.vue'

const createSkillsDisplayLocalRoutes = (skillsDisplayChildRoutes) => {
  return {
    path: '/progress-and-rankings/projects/:projectId',
    component: MyProjectSkillsPage,
    name: 'MyProjectSkillsPage',
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

export default createSkillsDisplayLocalRoutes
