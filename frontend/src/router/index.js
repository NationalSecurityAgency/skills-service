import Vue from 'vue';
import Router from 'vue-router';
import HomePage from '@/components/HomePage';
import ProjectPage from '@/components/projects/ProjectPage';
import SubjectPage from '@/components/subjects/SubjectPage';
import BadgePage from '@/components/badges/BadgePage';
import SkillPage from '@/components/skills/SkillPage';
import UserPage from '@/components/users/UserPage';
import store from '@/store/store';

Vue.use(Router);

const router = new Router({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'HomePage',
      component: HomePage,
      meta: {
        breadcrumb: {
          label: 'Home',
          utils: {
            iconClass: 'fas fa-home',
          },
        },
      },
    },
    {
      path: '/projects/:projectId',
      name: 'ProjectPage',
      component: ProjectPage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/subjects/:subjectId',
      name: 'SubjectPage',
      component: SubjectPage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/badges/:badgeId',
      name: 'BadgePage',
      component: BadgePage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/subjects/:subjectId/skills/:skillId',
      name: 'SkillPage',
      component: SkillPage,
      meta: { requiresAuth: true },
    },
    {
      path: '/projects/:projectId/user/:userId',
      name: 'UserPage',
      component: UserPage,
      meta: { requiresAuth: true },
    },
  ],
});

const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId;
const isLoggedIn = () => store.getters.isAuthenticated;

router.beforeEach((to, from, next) => {
  if (isActiveProjectIdChange(to, from)) {
    store.commit('currentProjectId', to.params.projectId);
  }
  if (to.matched.some(record => record.meta.requiresAuth)) {
    // this route requires auth, check if logged in if not, redirect to login page.
    if (!isLoggedIn()) {
      next({
        path: '/',
        query: { redirect: to.fullPath },
      });
    } else {
      next();
    }
  } else {
    next();
  }
});

export default router;
