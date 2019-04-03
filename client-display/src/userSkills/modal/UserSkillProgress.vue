<template>
    <div class="row">
        <div class="col-xs-9">
            <popper trigger="hover" :options="{ placement: 'top' }">
                <div slot="reference" v-on:click="progressBarClicked">
                    <vertical-progress v-if="progress.total === 100"
                                       total-progress-bar-color="#59ad52" before-today-bar-color="#59ad52"
                                       :total-progress="progress.total"
                                       :total-progress-before-today="progress.totalBeforeToday"/>
                    <vertical-progress v-if="skill.points !== skill.totalPoints && progress.total !== 100"
                                       :total-progress="progress.total"
                                       :total-progress-before-today="progress.totalBeforeToday"
                                       :is-locked="locked"/>
                </div>
                <div class="popper">
                    <my-progress-summary v-if="!locked" :user-skills="skill" summary-type="skill"/>
                    <div v-else>
                        <skill-is-locked-message :user-skill="skill"></skill-is-locked-message>
                    </div>
                </div>
            </popper>
            <div v-if="showDescription && skill.description" class="user-skill-subject-description text-muted">
                <p v-if="skill.description.description" v-html="parseMarkdown(skill.description.description)"/>
                <ul v-if="skill.description.examples">
                    Examples:
                    <li v-for="(example, index) in skill.description.examples"
                        :key="`unique-example-${index}`" v-html="example"/>
                </ul>
                <div v-if="skill.description.href" class="user-skill-description-href">
                    <strong>Need help?</strong>
                    <a :href="skill.description.href" target="_blank">
                        Click here!
                    </a>
                </div>
            </div>
        </div>
        <div class="col-xs-3">
            <div class="col-xs-7">
                <popper trigger="hover" :options="{ placement: 'left' }">
                    <div slot="reference" class="skill-label text-left">
                        {{ skill.points | number }} / {{ skill.totalPoints | number }}
                    </div>
                    <div class="popper">
                        <div>
                            {{ skill.pointIncrement | number }} points maximum earned per day
                        </div>
                    </div>
                </popper>
            </div>
            <div v-if="skill.points === skill.totalPoints" class="col-xs-3">
                <popper trigger="hover" :options="{ placement: 'left' }">
                    <div slot="reference" class="fa fa-check item-complete-icon"/>
                    <div class="popper">
                        <div>Skill complete</div>
                    </div>
                </popper>
            </div>
        </div>
    </div>
</template>

<script>
    import MyProgressSummary from '@/userSkills/MyProgressSummary.vue';
    import VerticalProgress from '@/common/progress/VerticalProgress.vue';
    import SkillIsLockedMessage from '@/userSkills/SkillIsLockedMessage.vue';

    import Popper from 'vue-popperjs';
    import marked from 'marked';

    export default {
        name: 'UserSkillProgress',
        components: {
            SkillIsLockedMessage,
            MyProgressSummary,
            VerticalProgress,
            Popper,
        },
        props: {
            skill: Object,
            showDescription: Boolean,
        },
        computed: {
            progress() {
                return {
                    total: (this.skill.points / this.skill.totalPoints) * 100,
                    totalBeforeToday: ((this.skill.points - this.skill.todaysPoints) / this.skill.totalPoints) * 100,
                };
            },
            locked() {
                return this.skill.dependencyInfo && !this.skill.dependencyInfo.achieved;
            },
        },
        methods: {
            parseMarkdown(markdown) {
                return marked(markdown);
            },
            progressBarClicked(skill) {
                this.$emit('progressbar-clicked', skill);
            },
        },
    };
</script>

<style scoped>
    .user-skill-subject-description {
        text-align: left;
        font-style: italic;
        padding: 10px;
    }

    .user-skill-description-href {
        margin-top: 8.5px;
    }

</style>
