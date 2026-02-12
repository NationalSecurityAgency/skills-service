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
import QuizPage from '@/components/quiz/QuizPage.vue';
import Questions from '@/components/quiz/testCreation/Questions.vue';
import QuizMetrics from '@/components/quiz/metrics/QuizMetrics.vue';
import QuizSettings from '@/components/quiz/QuizSettings.vue';
import QuizSkillsPage from '@/components/quiz/QuizSkillsPage.vue';
import QuizRunsHistoryPage from '@/components/quiz/runsHistory/QuizRunsHistoryPage.vue';
import QuizSingleRunPage from '@/components/quiz/runsHistory/QuizSingleRunPage.vue';
import QuizAccessPage from '@/components/quiz/access/QuizAccessPage.vue';
import UserActionsPage from "@/components/userActions/UserActionsPage.vue";
import GradeQuizzesPage  from "@/components/quiz/grade/GradeQuizzesPage.vue";
import VideoConfigPage from "@/components/video/VideoConfigPage.vue";
import SlidesConfigPage from "@/components/slides/SlidesConfigPage.vue";
import AiGraderConfigPage from "@/components/quiz/testCreation/AiGraderConfigPage.vue";

const createQuizRoutes = () => {
  return {
    name: 'QuizOverview',
    path: '/administrator/quizzes/:quizId',
    component: QuizPage,
    meta: {
      requiresAuth: true,
      announcer: {
        message: 'Manage Quiz',
      },
    },
    children: [{
      name: 'Questions',
      path: '',
      component: Questions,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Quiz or Survey',
        },
      },
      alias: 'questions'
    }, {
        name: 'QuizConfigureSlides',
        path: 'config-slides',
        component: SlidesConfigPage,
        meta: {
            requiresAuth: true,
            announcer: {
                message: 'Configure Slides',
            },
        },
        props: true,
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
      name: 'GradeQuizzesPage',
      path: 'grading',
      component: GradeQuizzesPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Grade Quizzes',
        },
      },
    },{
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
    }, {
      name: 'ConfigureQuizVideo',
      path: 'questions/:questionId/config-video',
      component: VideoConfigPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Configure Audio/Video',
        },
      },
      props: true,
      alias: 'questions/:questionId'
    }, {
      name: 'AiGraderConfigPage',
      path: 'questions/:questionId/ai-grader',
      component: AiGraderConfigPage,
      meta: {
        requiresAuth: true,
        announcer: {
          message: 'Configure AI Grader',
        },
      },
      props: true,
    },],
  }
}

export default createQuizRoutes
