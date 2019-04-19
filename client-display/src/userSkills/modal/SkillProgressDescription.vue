<template>
    <small v-if="skill" class="font-italic pl-2 py-3 text-left text-muted">
        <div v-if="locked" class="mb-2">
            Skill has <b>{{ skill.dependencyInfo.numDirectDependents}}</b> direct dependent(s).
            Click <i class="fas fa-lock icon"></i> to see its dependencies.
        </div>

        <div>
            Points Earned: Overall: {{ skill.points }} Today: {{ skill.todaysPoints }}
        </div>

        <div class="my-2">
            Per day allowance: {{ skill.pointIncrement | number }} points
        </div>

        <p v-if="skill.description.description" v-html="parseMarkdown(skill.description.description)"/>
        <ul v-if="skill.description.examples">
            Examples:
            <li v-for="(example, index) in skill.description.examples" :key="`unique-example-${index}`"
                v-html="example"/>
        </ul>

        <div v-if="skill.description.href" class="user-skill-description-href">
            <strong>Need help?</strong>
            <a :href="skill.description.href" target="_blank">
                Click here!
            </a>
        </div>
    </small>
</template>

<script>
    import marked from 'marked';

    export default {
        name: 'SkillProgressDescription',
        props: {
            skill: Object,
            locked: Boolean,
        },
        methods: {
            parseMarkdown(markdown) {
                return marked(markdown);
            },
        },
    };
</script>

<style scoped>

</style>
