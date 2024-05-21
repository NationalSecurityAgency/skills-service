import AdminHomePage from '@/components/AdminHomePage.vue'
import MyProjects from '@/components/projects/MyProjects.vue'
import QuizDefinitionsPage from '@/components/quiz/QuizDefinitionsPage.vue'
import UserActionsPage from '@/components/userActions/UserActionsPage.vue';
import InceptionSkills from '@/components/inception /InceptionSkills.vue'

const createAdminRoutes = () => {
  return {
    path: '/administrator',
    component: AdminHomePage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Project Administrator'
      }
    },
    children: [
      {
        name: 'AdminHomePage',
        path: '',
        component: MyProjects,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Project Administrator'
          }
        }
      }, {
        name: 'QuizzesAndSurveys',
        path: 'quizzes',
        component: QuizDefinitionsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'Quizzes and Surveys',
          },
        },
      }, {
        name: 'UserActions',
        path: 'userActions',
        component: UserActionsPage,
        meta: {
          requiresAuth: true,
          announcer: {
            message: 'User Actions History',
          },
        },
      }
    ]
  }
}

export default createAdminRoutes
