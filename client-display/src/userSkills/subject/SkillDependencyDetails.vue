<template>
    <div v-if="!loading" class="container">
        <ribbon>Skill Dependency</ribbon>
        <skill-dependency-card :skill="skill" :has-ok-button="false"></skill-dependency-card>
    </div>
</template>

<script>
    import SkillDependencyCard from '@/userSkills/subject/SkillDependencyCard.vue';
    import Ribbon from '@/common/ribbon/Ribbon.vue';
    import UserSkillsService from '@/userSkills/service/UserSkillsService';

    export default {
        name: 'SkillDependencyDetails',
        components: {
            SkillDependencyCard,
            Ribbon,
        },
        data() {
            return {
                loading: true,
                skill: {},
            };
        },
        mounted() {
            UserSkillsService.getSkillSummary(this.$route.params.skillId)
                .then((res) => {
                    this.skill = res;
                    this.loading = false;
                });
        },
    };
</script>

<style scoped>

</style>
