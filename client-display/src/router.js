import Vue from 'vue';
import VueRouter from 'vue-router';

import SkillsEntry from '@/SkillsEntry.vue';
import SubjectDetails from '@/userSkills/subject/SubjectDetails.vue';
import MyRankDetails from '@/userSkills/myRank/MyRankDetails.vue';
import BadgesDetails from '@/userSkills/badge/BadgesDetails.vue';
import BadgeDetails from '@/userSkills/badge/BadgeDetails.vue';
import ErrorPage from '@/userSkills/ErrorPage.vue';
import store from '@/store';

import { debounce } from 'lodash';

Vue.use(VueRouter);

// divide the app into smaller chunks and only load a component from the server when it’s needed
const SkillDetails = () => import('@/userSkills/skill/SkillDetails.vue');

const router = new VueRouter({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: SkillsEntry,
      props: true,
    },
    {
      path: '',
      name: 'error',
      component: ErrorPage,
      props: true,
    },
    {
      path: '/subjects/:subjectId',
      component: SubjectDetails,
      name: 'subjectDetails',
      props: true,
    },
    {
      path: '/badges',
      component: BadgesDetails,
      name: 'badges',
      props: true,
    },
    {
      path: '/badges/:badgeId',
      component: BadgeDetails,
      name: 'badgeDetails',
      props: true,
    },
    {
      path: '/skills/:skillId',
      component: SkillDetails,
      name: 'skillDetails',
    },
    {
      path: '/skills/crossProject/:crossProjectId/:skillId',
      component: SkillDetails,
      name: 'crossProjectSkillDetails',
    },
    {
      path: '/rank',
      component: MyRankDetails,
      name: 'myRankDetails',
      props: true,
    },
    {
      path: '*',
      component: SkillsEntry,
    },
  ],
});

router.afterEach(debounce(() => {
  if (process.env.NODE_ENV !== 'development') {
    store.state.parentFrame.emit('route-changed');
  }
}, 250));

export default router;
