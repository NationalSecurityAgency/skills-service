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
    import MyBadgesDetails from '@/userSkills/badge/MyBadgesDetails.vue';
    import BadgesCatalog from '@/userSkills/badge/BadgesCatalog.vue';
    import UserSkillsService from '@/userSkills/service/UserSkillsService';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';
    import SkillsTitle from '@/common/utilities/SkillsTitle.vue';

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
                    this.achievedBadges = this.badges.filter(item => item.badgeAchieved);
                    this.loading = false;
                });
        },
    };
</script>

<style scoped>

</style>
