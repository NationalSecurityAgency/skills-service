<template>
    <div v-if="skill" class="text-left text-muted pb-md-2">
        <div v-if="locked" class="row justify-content-center pb-3 locked-text">
            <div class="col-12 col-md-9 text-muted">
                *** Skill has <b>{{ skill.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
                Click <i class="fas fa-lock icon"></i> to see its dependencies. ***
            </div>
        </div>
        <small v-else>
            <skill-summary-cards class="mb-3" :skill="skill" :short-sub-titles="true"/>
        </small>


        <div v-if="skill.description">
            <p v-if="skill.description.description" class="text-primary skills-text-description">
                <markdown-text :text="skill.description.description"/>
            </p>
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
        </div>

        <hr class="mb-2"/>
    </div>
</template>

<script>
    import SkillSummaryCards from '@/userSkills/skill/progress/SkillSummaryCards.vue';
    import MarkdownText from '@/common/utilities/MarkdownText.vue';

    export default {
        name: 'SkillProgressDescription',
        components: { SkillSummaryCards, MarkdownText },
        props: {
            skill: Object,
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
