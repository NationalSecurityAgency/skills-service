<template>
    <small v-if="skill" class="font-italic text-left text-muted pb-md-2">
        <div v-if="locked" class="row justify-content-center pb-3 locked-text">
            <div class="col-12 col-md-9">
                *** Skill has <b>{{ skill.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
                Click <i class="fas fa-lock icon"></i> to see its dependencies. ***
            </div>
        </div>
        <div v-else>
            <skill-summary-cards class="mb-3" :skill="skill"/>
        </div>


        <p v-if="skill.description.description" v-html="parseMarkdown(skill.description.description)"/>
        <ul v-if="skill.description.examples">
            Examples:
            <li v-for="(example, index) in skill.description.examples" :key="`unique-example-${index}`"
                v-html="example"/>
        </ul>

        <div v-if="skill.description.href" class="user-skill-description-href mb-3 text-center text-md-left">
            <strong>Need help?</strong>
            <a :href="skill.description.href" target="_blank">
                Click here!
            </a>
        </div>

        <hr class="mb-2"/>
    </small>
</template>

<script>
    import marked from 'marked';
    import SkillSummaryCards from '@/userSkills/skill/SkillSummaryCards.vue';

    export default {
        name: 'SkillProgressDescription',
        components: { SkillSummaryCards },
        props: {
            skill: Object,
        },
        methods: {
            parseMarkdown(markdown) {
                return marked(markdown);
            },
        },
        computed: {
            locked() {
                return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
            },
        },
    };
</script>

<style scoped>
    .locked-text {
        font-size: 0.8rem;
        color: #383838;
    }
</style>
