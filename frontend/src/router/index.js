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
    },
    {
      path: '/projects/:projectId/subjects/:subjectId',
      name: 'SubjectPage',
      component: SubjectPage,
    },
    {
      path: '/projects/:projectId/badges/:badgeId',
      name: 'BadgePage',
      component: BadgePage,
    },
    {
      path: '/projects/:projectId/subjects/:subjectId/skills/:skillId',
      name: 'SkillPage',
      component: SkillPage,
    },
    {
      path: '/projects/:projectId/user/:userId',
      name: 'UserPage',
      component: UserPage,
    },
  ],
});

const isActiveProjectIdChange = (to, from) => to.params.projectId !== from.params.projectId;

router.beforeEach((to, from, next) => {
  if (isActiveProjectIdChange(to, from)) {
    store.commit('currentProjectId', to.params.projectId);
  }
  next();
});

export default router;
