/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <div slot="subSubTitle" v-if="tags">
        <span v-for="(tag, index) in tags" :key="index">
          <span class="text-muted">{{tag.label}}: </span>
          <span v-for="(value, vIndex) in tag.value" :key="vIndex">
            <router-link
              :to="{ name: 'UserTagMetrics', params: { projectId: projectId, tagKey: tag.key, tagFilter: value } }"
              class="text-info mb-0 pb-0 preview-card-title"
              :aria-label="`View metrics for ${value}`" role="link">{{ value }}</router-link>
              <span v-if="vIndex < tag.value.length - 1">, </span>
          </span>
          <span v-if="index < tags.length - 1">; </span>
        </span>
      </div>
    </page-header>

    <navigation v-if="userIdForDisplay" :nav-items="getNavItems()">
    </navigation>
    <scroll-to-top />
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';

  import ScrollToTop from '@/common-components/utilities/ScrollToTop';
  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';
  import UsersService from './UsersService';

  const { mapActions, mapGetters } = createNamespacedHelpers('users');

  export default {
    name: 'UserPage',
    components: {
      PageHeader,
      Navigation,
      ScrollToTop,
    },
    data() {
      return {
        userTitle: '',
        userIdForDisplay: '',
        isLoading: true,
        tags: '',
        projectId: this.$route.params.projectId,
      };
    },
    created() {
      this.userTitle = this.$route.params.userId;
      this.userIdForDisplay = this.$route.params.userId;
      let userTags;
      let userDetails;

      if (this.$store.getters.config.userPageTagsToDisplay) {
        userTags = UsersService.getUserTags(this.$route.params.userId).then((response) => {
          this.tags = this.processUserTags(response);
        });
      }

      if (this.$store.getters.isPkiAuthenticated) {
        UsersService.getUserInfo(this.$route.params.projectId, this.$route.params.userId)
          .then((result) => {
            this.userIdForDisplay = result.userIdForDisplay;
            this.userTitle = result.first && result.last ? `${result.first} ${result.last}` : result.userIdForDisplay;
            userDetails = this.loadUserDetails();
          });
      } else {
        userDetails = this.loadUserDetails();
      }
      userDetails = this.loadUserDetails();

      Promise.all([userTags, userDetails]).finally(() => {
        this.isLoading = false;
      });
    },
    computed: {
      ...mapGetters([
        'numSkills',
        'userTotalPoints',
      ]),
      headerOptions() {
        return {
          icon: 'fas fa-user skills-color-users',
          title: `USER: ${this.userTitle}`,
          subTitle: `ID: ${this.userIdForDisplay}`,
          stats: [{
            label: 'Skills',
            count: this.numSkills,
            icon: 'fas fa-graduation-cap skills-color-skills',
          }, {
            label: 'Points',
            count: this.userTotalPoints,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }],
        };
      },
    },
    methods: {
      ...mapActions([
        'loadUserDetailsState',
      ]),
      loadUserDetails() {
        return this.loadUserDetailsState({ projectId: this.$route.params.projectId, userId: this.$route.params.userId });
      },
      processUserTags(userTags) {
        const userPageTags = this.$store.getters.config.userPageTagsToDisplay;
        const tags = [];
        if (userPageTags) {
          const tagSections = userPageTags.split('|');
          tagSections.forEach((section) => {
            const [key, label] = section.split('/');
            tags.push({
              key, label,
            });
          });
        }

        const processedTags = [];
        tags.forEach((tag) => {
          const userTag = userTags.filter((ut) => ut.key === tag.key);
          if (userTag) {
            const values = userTag.map((ut) => ut.value);
            if (values.length > 0) {
              processedTags.push({ label: tag.label, key: tag.key, value: values });
            }
          }
        });
        return processedTags;
      },
      getNavItems() {
        const hasSubject = this.$route.params.subjectId || false;
        const hasSkill = this.$route.params.skillId || false;
        const hasBadge = this.$route.params.badgeId || false;

        let displayPage = 'ClientDisplayPreview';
        let skillsPage = 'UserSkillEvents';

        if (hasSkill) {
          displayPage = `${displayPage}Skill`;
          skillsPage = `${skillsPage}Skill`;
        } else if (hasSubject) {
          displayPage = `${displayPage}Subject`;
          skillsPage = `${skillsPage}Subject`;
        } else if (hasBadge) {
          displayPage = `${displayPage}Badge`;
          skillsPage = `${skillsPage}Badge`;
        }

        return [
          { name: 'Client Display', iconClass: 'fa-user skills-color-skills', page: `${displayPage}` },
          { name: 'Performed Skills', iconClass: 'fa-award skills-color-events', page: `${skillsPage}` },
        ];
      },
    },
  };
</script>

<style scoped>
  .version-select {
    width: 7rem;
  }
</style>
