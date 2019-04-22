<template>
    <div class="container">
        <ribbon>
            Badges
        </ribbon>

        <skills-spinner v-if="loading" :loading="loading" class="mt-5"/>

        <div v-if="!loading">
            <my-badges-details :badges="achievedBadges"></my-badges-details>
            <badges-catalog v-if="badges && badges.length > 0" class="mt-3" :badges="badges"></badges-catalog>
        </div>
    </div>
</template>

<script>
    import Ribbon from '@/common/ribbon/Ribbon.vue';
    import MyBadgesDetails from '@/userSkills/badge/MyBadgesDetails.vue';
    import BadgesCatalog from '@/userSkills/badge/BadgesCatalog.vue';
    import UserSkillsService from '@/userSkills/service/UserSkillsService';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';

    export default {
        name: 'BadgesDetails',
        components: {
            SkillsSpinner,
            BadgesCatalog,
            MyBadgesDetails,
            Ribbon,
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
                    this.achievedBadges = this.badges.filter(item => item.badgeAchieved);
                    this.loading = false;
                });
        },
    };
</script>

<style scoped>

</style>
