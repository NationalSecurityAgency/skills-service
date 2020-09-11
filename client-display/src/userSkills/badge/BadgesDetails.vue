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
    <div class="container">
        <skills-spinner :loading="loading"/>

        <div v-if="!loading">
            <skills-title>Badges</skills-title>

            <my-badges-details :badges="achievedBadges"></my-badges-details>
            <badges-catalog v-if="badges && badges.length > 0" class="mt-3" :badges="badges"></badges-catalog>
        </div>
    </div>
</template>

<script>
  import MyBadgesDetails from '@/userSkills/badge/MyBadgesDetails';
  import BadgesCatalog from '@/userSkills/badge/BadgesCatalog';
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
        achievedBadges: [],
      };
    },
    mounted() {
      UserSkillsService.getBadgeSummaries()
        .then((res) => {
          this.badges = res;
          this.achievedBadges = this.badges.filter((item) => item.badgeAchieved);
          this.loading = false;
        });
    },
  };
</script>

<style scoped>

</style>
