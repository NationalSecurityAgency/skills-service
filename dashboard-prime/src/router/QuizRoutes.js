import QuizPage from '@/components/quiz/QuizPage.vue';
import Questions from '@/components/quiz/testCreation/Questions.vue';
import QuizMetrics from '@/components/quiz/metrics/QuizMetrics.vue';
import QuizSettings from '@/components/quiz/QuizSettings.vue';
import QuizSkillsPage from '@/components/quiz/QuizSkillsPage.vue';
import QuizRunsHistoryPage from '@/components/quiz/runsHistory/QuizRunsHistoryPage.vue';
import QuizSingleRunPage from '@/components/quiz/runsHistory/QuizSingleRunPage.vue';
// import QuizRun from '@/components/quiz/QuizRunInDashboard.vue';
import QuizAccessPage from '@/components/quiz/access/QuizAccessPage.vue';
import UserActionsPage from "@/components/userActions/UserActionsPage.vue";

const createQuizRoutes = () => {
  return {
    path: '/administrator/quizzes/:quizId',
    component: QuizPage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Manage Test',
      },
    },
    children: [{
      name: 'Questions',
      path: '',
      component: Questions,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Test or Survey',
        },
      },
    }, {
      name: 'QuizMetrics',
      path: 'results',
      component: QuizMetrics,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Quiz Metrics',
        },
      },
    }, {
      name: 'QuizSkillsPage',
      path: 'skills',
      component: QuizSkillsPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Quiz Skills',
        },
      },
    }, {
      name: 'QuizRunsHistoryPage',
      path: 'runs',
      component: QuizRunsHistoryPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Quiz Runs',
        },
      },
    }, {
      name: 'QuizSingleRunPage',
      path: 'runs/:runId',
      component: QuizSingleRunPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Quiz Single Run',
        },
      },
    }, {
      name: 'QuizAccessPage',
      path: 'access',
      component: QuizAccessPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Quiz Access',
        },
      },
    }, {
      name: 'QuizSettings',
      path: 'settings',
      component: QuizSettings,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Quiz Settings',
        },
      },
    }, {
      name: 'QuizActivityHistory',
      path: 'activityHistory',
      component: UserActionsPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'User Activity History',
        },
      },
    }],
  }
}

export default createQuizRoutes
