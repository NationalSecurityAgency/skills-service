import Vue from 'vue';
import VueRouter from 'vue-router';
// import VueBreadcrumbs from 'vue-2-breadcrumbs';

import SkillsEntry from '@/SkillsEntry.vue';
import SubjectDetails from '@/userSkills/subject/SubjectDetails.vue';
import SkillDependencies from '@/userSkills/subject/SkillDependencies.vue';
import MyRankDetails from '@/userSkills/myRank/MyRankDetails.vue';
import BadgesDetails from '@/userSkills/badge/BadgesDetails.vue';
import BadgeDetails from '@/userSkills/badge/BadgeDetails.vue';


Vue.use(VueRouter);
// Vue.use(VueBreadcrumbs);

const router = new VueRouter({
  mode: 'history',
  routes: [
    {
      path: '/',
      name: 'home',
      component: SkillsEntry,
    },
    {
      path: '/subject/:subjectId',
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
      path: '/badgeDetails/:badgeId',
      component: BadgeDetails,
      name: 'badgeDetails',
      props: true,
    },
    {
      path: '/skill/dependencies',
      component: SkillDependencies,
      name: 'skillDependencies',
      props: true,
    },
    {
      path: '/myrank',
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

router.afterEach(() => {
  window.parent.postMessage('skills::route-changed::{}', '*');
});

export default router;
