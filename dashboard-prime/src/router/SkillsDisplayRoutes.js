import AdminHomePage from '@/components/AdminHomePage.vue'
import MyProjects from '@/components/projects/MyProjects.vue'
import QuizDefinitionsPage from '@/components/quiz/QuizDefinitionsPage.vue'
import SkillsDisplay from '@/skills-display/SkillsDisplayInIframe.vue'

const createSkillsDisplayRoutes = () => {
  return {
    path: '/static/clientPortal/index.html',
    name: 'SkillsDisplayHome',
    component: SkillsDisplay,
    meta: {
      requiresAuth: true,
      nonAdmin: true,
      announcer: {
        message: 'Skills Display'
      }
    }
  }
}

export default createSkillsDisplayRoutes
