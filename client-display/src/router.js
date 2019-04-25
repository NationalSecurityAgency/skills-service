import Vue from 'vue';
import VueRouter from 'vue-router';
// import VueBreadcrumbs from 'vue-2-breadcrumbs';

import SkillsEntry from '@/SkillsEntry.vue';
import SubjectDetails from '@/userSkills/subject/SubjectDetails.vue';
import SkillDetails from '@/userSkills/skill/SkillDetails.vue';
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

router.afterEach(() => {
  window.parent.postMessage('skills::route-changed::{}', '*');
});

export default router;
