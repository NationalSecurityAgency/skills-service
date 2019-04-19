<template>
    <div class="card mt-2">
        <div class="card-body">
            <skills-subject-skill-row v-if="subject.skills && subject.skills.length > 0"
                                      v-for="(skill, index) in subject.skills" :key="`unique-skill-${index}`"
                                      :skill="skill" :show-description="showDescriptionsInternal"/>
            <no-data-yet v-if="!(subject.skills && subject.skills.length > 0)" class="my-2"
                    title="Skills have not been added yet." sub-title="Please contact this project's administrator."/>
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
    import NoDataYet from '@/common/utilities/NoDataYet.vue';

    export default {
        components: {
            NoDataYet,
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
