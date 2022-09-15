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
import UserSkillsService from './userSkills/service/UserSkillsService';

Vue.use(VueRouter);

const projectPlaceholder = '##PROJECT##';
const projectPlaceholderRegex = new RegExp(projectPlaceholder, 'g');
const subjectPlaceholder = '##SUBJECT##';
const subjectPlaceholderRegex = new RegExp(subjectPlaceholder, 'g');
const groupPlaceholder = '##GROUP##';
const groupPlaceholderRegex = new RegExp(groupPlaceholder, 'g');
const skillPlaceholder = '##SKILL##';
const skillPlaceholderRegex = new RegExp(skillPlaceholder, 'g');

const router = new VueRouter({
  mode: 'abstract',
  routes: [
    {
      path: '/',
      name: 'home',
      component: SkillsEntry,
      props: true,
      meta: {
        title: 'Overview',
      },
    },
    {
      path: '',
      name: 'error',
      component: ErrorPage,
      props: true,
      meta: {
        title: 'Error',
      },
    },
    {
      path: '/subjects/:subjectId',
      component: SubjectDetails,
      name: 'subjectDetails',
      props: true,
      meta: {
        title: `${subjectPlaceholder} Overview`,
      },
    },
    {
      path: '/badges',
      component: BadgesDetails,
      name: 'badges',
      props: true,
      meta: {
        title: 'Badges Overview',
      },
    },
    {
      path: '/badges/:badgeId',
      component: BadgeDetails,
      name: 'badgeDetails',
      props: true,
      meta: {
        title: 'Badge Details',
      },
    },
    {
      path: '/badges/global/:badgeId',
      component: GlobalBadgeDetails,
      name: 'globalBadgeDetails',
      props: true,
      meta: {
        title: 'Global Badge Details',
      },
    },
    {
      path: '/subjects/:subjectId/skills/:skillId',
      component: SkillDetails,
      name: 'skillDetails',
      meta: {
        title: `${skillPlaceholder}} Details`,
      },
    },
    {
      path: '/subjects/:subjectId/skills/:skillId/crossProject/:crossProjectId/:dependentSkillId',
      component: SkillDetails,
      name: 'crossProjectSkillDetails',
      meta: {
        title: `Cross ${projectPlaceholder} ${skillPlaceholder} Details`,
      },
    },
    {
      path: '/subjects/:subjectId/skills/:skillId/dependency/:dependentSkillId',
      component: SkillDetails,
      name: 'dependentSkillDetails',
      meta: {
        title: `Dependant ${skillPlaceholder} Details`,
      },
    },
    {
      path: '/badges/:badgeId/skills/:skillId',
      component: SkillDetails,
      name: 'badgeSkillDetails',
      meta: {
        title: `Badge ${skillPlaceholder} Details`,
      },
    },
    {
      path: '//badges/global/:badgeId/skills/:skillId',
      component: SkillDetails,
      name: 'globalBadgeSkillDetails',
      meta: {
        title: `Global Badge ${skillPlaceholder} Details`,
      },
    },
    {
      path: '/rank',
      component: MyRankDetails,
      name: 'myRankDetails',
      meta: {
        title: 'My Rank',
      },
      props: true,
    },
    {
      path: '/subjects/:subjectId/rank',
      component: MyRankDetails,
      name: 'subjectRankDetails',
      props: true,
      meta: {
        title: `My ${subjectPlaceholder} Rank`,
      },
    },
    {
      path: '*',
      component: SkillsEntry,
      meta: {
        title: 'Overview',
      },
    },
  ],
});

const isWildcardMatch = (matched) => matched.filter((item) => item.path === '*').length > 0;
const getMetaTitle = (to) => {
  if (store.getters && store.getters.config) {
    let metaTitle = to.meta.title.replaceAll(projectPlaceholderRegex, store.getters.config.projectDisplayName);
    metaTitle = metaTitle.replaceAll(subjectPlaceholderRegex, store.getters.config.subjectDisplayName);
    metaTitle = metaTitle.replaceAll(groupPlaceholderRegex, store.getters.config.groupDisplayName);
    return metaTitle.replaceAll(skillPlaceholderRegex, store.getters.config.skillDisplayName);
  }
  return to.meta.title;
};

router.beforeEach((to, from, next) => {
  if (store.state.internalBackButton && !to.params.previousRoute && to.meta.setPreviousRoute !== false && !isWildcardMatch(to.matched)) {
    const previousRoute = { ...from };
    const params = { ...to.params, ...{ previousRoute } };

    if (to.name === 'subjectDetails' && from.name === 'skillDetails') {
      params.jumpToLastViewed = true;
    }

    const updatedTo = { ...to, ...{ params }, replace: true };

    next(updatedTo);
  } else {
    next();
  }
});

let siteTitle;
router.afterEach(debounce((to) => {
  // Use next tick to handle router history correctly
  // see: https://github.com/vuejs/vue-router/issues/914#issuecomment-384477609
  if (to && to.meta && to.meta.title) {
    const metaTitle = getMetaTitle(to);
    if (!siteTitle) {
      siteTitle = document.title;
    }
    Vue.nextTick(() => {
      if (siteTitle) {
        document.title = `${siteTitle} - ${metaTitle}`;
      } else {
        document.title = metaTitle;
      }
    });
  }
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
  if (store.getters && store.getters.config
      && (store.getters.config.enablePageVisitReporting === true || store.getters.config.enablePageVisitReporting === 'true')) {
    UserSkillsService.reportPageVisit(to.path, to.fullPath);
  }
}, 250));

export default router;
