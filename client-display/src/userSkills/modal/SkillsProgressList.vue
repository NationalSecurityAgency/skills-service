<template>
    <div class="card mt-2">
<!--        <div class="card-header">-->
<!--            <div class="row py-0 my-0">-->
<!--                <div class="col py-0 my-0">-->
<!--                    <h6 class="card-title mb-0 float-left">Skills</h6>-->
<!--                </div>-->
<!--                <div class="col text-right py-0 my-0">-->
<!--                    <span class="text-muted pr-1">Descriptions:</span>-->
<!--                    <toggle-button class="mt-1" v-model="showDescriptionsInternal" :labels="{ checked: 'On', unchecked: 'Off' }"/>-->
<!--                </div>-->
<!--            </div>-->
<!--        </div>-->

        <div class="card-body">
                <skills-subject-skill-row v-for="(skill, index) in subject.skills" :key="`unique-skill-${index}`"
                        :skill="skill" :show-description="showDescriptionsInternal"/>
        </div>

        <div class="card-footer">
            <div class="row">
                <div class="col">
                        <span v-if="helpTipHref" class="float-left">
                            Need help? <a :href="helpTipHref" target="_blank">Click here!</a>
                        </span>
                </div>
                <div class="col text-right">
                    <span class="text-muted pr-1">Descriptions:</span>
                    <toggle-button class="" v-model="showDescriptionsInternal" :labels="{ checked: 'On', unchecked: 'Off' }"/>
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
            };
        },
        mounted() {
            this.showDescriptionsInternal = this.showDescriptions;
        },
    };
</script>

<style scoped>
</style>
