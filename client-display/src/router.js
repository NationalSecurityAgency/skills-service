/*
 * Copyright 2020 SkillTree
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
import Vue from 'vue';
import VueRouter from 'vue-router';

import debounce from 'lodash/debounce';

import SkillsEntry from '@/SkillsEntry';
import SubjectDetails from '@/userSkills/subject/SubjectDetails';
import MyRankDetails from '@/userSkills/myRank/MyRankDetails';
import SkillDetails from '@/userSkills/skill/SkillDetails';
import BadgesDetails from '@/userSkills/badge/BadgesDetails';
import GlobalBadgeDetails from '@/userSkills/badge/GlobalBadgeDetails';
import BadgeDetails from '@/userSkills/badge/BadgeDetails';
import ErrorPage from '@/userSkills/ErrorPage';
import store from '@/store/store';

Vue.use(VueRouter);

const router = new VueRouter({
  mode: 'abstract',
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
      path: '/badges/global/:badgeId',
      component: GlobalBadgeDetails,
      name: 'globalBadgeDetails',
      props: true,
    },
    {
      path: '/subjects/:subjectId/skills/:skillId',
      component: SkillDetails,
      name: 'skillDetails',
    },
    {
      path: '/subjects/:subjectId/skills/:skillId/crossProject/:crossProjectId/:dependentSkillId',
      component: SkillDetails,
      name: 'crossProjectSkillDetails',
    },
    {
      path: '/subjects/:subjectId/skills/:skillId/dependency/:dependentSkillId',
      component: SkillDetails,
      name: 'dependentSkillDetails',
    },
    {
      path: '/badges/:badgeId/skills/:skillId',
      component: SkillDetails,
      name: 'badgeSkillDetails',
    },
    {
      path: '//badges/global/:badgeId/skills/:skillId',
      component: SkillDetails,
      name: 'globalBadgeSkillDetails',
    },
    {
      path: '/rank',
      component: MyRankDetails,
      name: 'myRankDetails',
      props: true,
    },
    {
      path: '/subjects/:subjectId/rank',
      component: MyRankDetails,
      name: 'subjectRankDetails',
      props: true,
    },
    {
      path: '*',
      component: SkillsEntry,
    },
  ],
});

const isWildcardMatch = (matched) => matched.filter((item) => item.path === '*').length > 0;

router.beforeEach((to, from, next) => {
  console.log(`going from [${from.fullPath}] to [${to.fullPath}]`);
  if (store.state.internalBackButton && !to.params.previousRoute && to.meta.setPreviousRoute !== false && !isWildcardMatch(to.matched)) {
    const previousRoute = { ...from };
    const params = { ...to.params, ...{ previousRoute } };
    const updatedTo = { ...to, ...{ params }, replace: true };
    next(updatedTo);
  } else {
    next();
  }
});

router.afterEach(debounce((to) => {
  if (process.env.NODE_ENV !== 'development' && store.state.parentFrame) {
    const params = {
     path: to.path,
     fullPath: to.fullPath,
     name: to.name,
     query: to.query,
     currentLocation: window.location.toString(),
     historySize: window.history.length,
    };
    store.state.parentFrame.emit('route-changed', params);
  }
}, 250));

export default router;
