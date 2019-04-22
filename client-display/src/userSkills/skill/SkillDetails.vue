<template>
    <div class="container">
        <div v-if="!loading.dependencies && !loading.skill">
            <skills-title>Skill Overview</skills-title>
            <skill-overview class="my-2" :skill="skill"></skill-overview>
            <skill-dependencies v-if="dependencies && dependencies.length > 0" :dependencies="dependencies"
                                :skill-id="$route.params.skillId"></skill-dependencies>
        </div>
        <div v-else>
            <skills-spinner :loading="loading.dependencies || loading.skill" class="mt-5"/>
        </div>
    </div>
</template>

<script>
    import UserSkillsService from '@/userSkills/service/UserSkillsService';
    import SkillDependencies from '@/userSkills/skill/dependencies/SkillDependencies.vue';
    import SkillOverview from '@/userSkills/skill/SkillOverview.vue';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';
    import SkillsTitle from '@/common/utilities/SkillsTitle.vue';

    export default {
        name: 'SkillDetails',
        components: {
            SkillsTitle,
            SkillOverview,
            SkillDependencies,
            SkillsSpinner,
        },
        data() {
            return {
                loading: {
                    dependencies: true,
                    skill: true,
                },
                dependencies: [],
                skill: {},
            };
        },
        mounted() {
            this.loadData();
        },
        watch: {
            $route: 'loadData',
        },
        methods: {
            loadData() {
                this.loading.dependencies = true;
                this.loading.skill = true;
                this.dependencies = [];
                this.skill = {};
                this.loadDependencies();
                this.loadSkillSummary();
            },
            loadDependencies() {
                UserSkillsService.getSkillDependencies(this.$route.params.skillId)
                    .then((res) => {
                        this.dependencies = res.dependencies;
                        this.loading.dependencies = false;
                    });
            },
            loadSkillSummary() {
                UserSkillsService.getSkillSummary(this.$route.params.skillId)
                    .then((res) => {
                        this.skill = res;
                        this.loading.skill = false;
                    });
            },
        },
    };
</script>

<style scoped>

</style>
