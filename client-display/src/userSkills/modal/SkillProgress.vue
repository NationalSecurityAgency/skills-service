<template>
    <div class="row">
        <div class="col-sm-9">
            <popper
                trigger="hover"
                :options="{ placement: 'top' }"
                :delay-on-mouse-over="250">
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
            <div v-if="showDescription && skill.description" class="font-italic pl-2 py-3 text-left text-muted">
                <small>
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
                </small>
            </div>
        </div>
        <div class="col-sm-3">
            <div class="col-sm-7">
                <popper trigger="hover" :options="{ placement: 'left' }">
                    <div slot="reference" class="text-left">
                        <small>{{ skill.points | number }} / {{ skill.totalPoints | number }}</small>
                    </div>
                    <div class="popper">
                        <div>
                            {{ skill.pointIncrement | number }} points maximum earned per day
                        </div>
                    </div>
                </popper>
            </div>
            <div v-if="skill.points === skill.totalPoints" class="col-sm-3">
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
