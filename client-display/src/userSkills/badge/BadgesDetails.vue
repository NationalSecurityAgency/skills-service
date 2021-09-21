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
        <skills-spinner :loading="loading"/>

        <div v-if="!loading">
            <skills-title>Badges</skills-title>

            <my-badges-details data-cy="achievedBadges" :badges="achievedBadges" :badgeRouterLinkGenerator="genLink"></my-badges-details>
            <badges-catalog class="mt-3"
              :noBadgesMessage="noCatalogMsg"
              :badges="unachievedBadges"
              :badgeRouterLinkGenerator="genLink"
              data-cy="availableBadges"
            >
            </badges-catalog>
        </div>
    </div>
</template>

<script>
  import BadgesCatalog from '@/common-components/badges/BadgesCatalog';
  import MyBadgesDetails from '@/common-components/badges/MyBadgesDetails';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import SkillsTitle from '@/common/utilities/SkillsTitle';

  export default {
    name: 'BadgesDetails',
    components: {
      SkillsTitle,
      SkillsSpinner,
      BadgesCatalog,
      MyBadgesDetails,
    },
    data() {
      return {
        loading: true,
        badges: [],
        unachievedBadges: [],
        achievedBadges: [],
      };
    },
    mounted() {
      this.loadBadges();
    },
    computed: {
      noCatalogMsg() {
        if (this.achievedBadges.length > 0 && this.unachievedBadges.length === 0) {
          return 'No Badges left to earn!';
        }
        return 'No Badges available';
      },
    },
    methods: {
      genLink(b) {
        return { name: b.global ? 'globalBadgeDetails' : 'badgeDetails', params: { badgeId: b.badgeId } };
      },
      loadBadges() {
        this.loading = true;
        UserSkillsService.getBadgeSummaries().then((res) => {
          this.badges = res;
          this.unachievedBadges = res.filter((badge) => badge.badgeAchieved === false);
          this.achievedBadges = res.filter((badge) => badge.badgeAchieved === true);
        }).finally(() => {
          this.loading = false;
        });
      },
    },
  };
</script>

<style scoped>

</style>
