<template>
    <div class="card mt-2">
        <div class="card-body">
            <skills-subject-skill-row v-if="subject.skills && subject.skills.length > 0"
                                      v-for="(skill, index) in subject.skills" :key="`unique-skill-${index}`"
                                      :skill="skill" :show-description="showDescriptionsInternal"/>

            <div v-if="!(subject.skills && subject.skills.length > 0)" class="row my-2 text-secondary">
                <div class="col">
                    <span class="fa-stack fa-3x " style="vertical-align: top;">
                      <i class="fas fa-circle fa-stack-2x"></i>
                      <i class="fas fa-battery-empty fa-stack-1x fa-inverse"></i>
                    </span>
                </div>
                <div class="w-100"></div>
                <div class="col pt-2">
                    <h3>No Skills have been added yet.</h3>
                </div>
                <div class="w-100"></div>
                <div class="col">
                    Please contact this project's administrator.
                </div>

            </div>
        </div>

        <div v-if="subject.skills && subject.skills.length > 0" class="card-footer">
            <div class="row">
                <div class="col">
                        <span v-if="helpTipHref" class="float-left">
                            Need help? <a :href="helpTipHref" target="_blank">Click here!</a>
                        </span>
                </div>
                <div class="col text-right">
                    <span class="text-muted pr-1">Skill details:</span>
                    <toggle-button class="" v-model="showDescriptionsInternal"
                                   :labels="{ checked: 'On', unchecked: 'Off' }"/>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import SkillsSubjectSkillRow from '@/userSkills/modal/SkillsSubjectSkillRow.vue';
    import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

    export default {
        components: {
            SkillsSubjectSkillRow,
            ToggleButton,
        },
        props: {
            subject: {
                type: Object,
                required: true,
            },
            showDescriptions: {
                type: Boolean,
                default: false,
            },
            helpTipHref: {
                type: String,
                required: false,
            },
        },
        data() {
            return {
                showDescriptionsInternal: false,
                hasSkills: false,
            };
        },
        mounted() {
            this.showDescriptionsInternal = this.showDescriptions;
        },
    };
</script>

<style scoped>
</style>
