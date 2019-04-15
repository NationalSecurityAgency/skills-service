<template>
    <div v-if="!loading" class="container">
        <ribbon :color="green">
            Badges
        </ribbon>

        <my-badges-details :badges="achievedBadges"></my-badges-details>

        <badges-catalog v-if="badges && badges.length > 0" class="mt-3" :badges="badges"></badges-catalog>

    </div>
</template>

<script>
    import Ribbon from '@/common/ribbon/Ribbon.vue';
    import MyBadgesDetails from '@/userSkills/badge/MyBadgesDetails.vue';
    import BadgesCatalog from '@/userSkills/badge/BadgesCatalog.vue';
    import UserSkillsService from '@/userSkills/service/UserSkillsService';

    export default {
        name: 'BadgesDetails',
        components: {
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
