<template>
    <div class="card mt-2">
        <div class="card-body">
            <skills-spinner :loading="loading"/>
            <div v-if="!loading">
                <skills-subject-skill-row v-if="skillsInternal && skillsInternal.length > 0"
                                          v-for="(skill, index) in skillsInternal" :key="`unique-skill-${index}`"
                                          :skill="skill" :show-description="showDescriptionsInternal"/>
                <no-data-yet v-if="!(skillsInternal && skillsInternal.length > 0)" class="my-2"
                        title="Skills have not been added yet." sub-title="Please contact this project's administrator."/>
            </div>
        </div>

        <div v-if="skillsInternal && skillsInternal.length > 0" class="card-footer">
            <div class="row">
                <div class="col">
                        <span v-if="helpTipHref" class="float-left text-muted">
                            Need help? <a :href="helpTipHref" target="_blank" rel="noopener">Click here!</a>
                        </span>
                </div>
                <div class="col text-right">
                    <span class="text-muted pr-1">Skill details:</span>
                    <toggle-button class="" v-model="showDescriptionsInternal" @change="onDetailsToggle"
                                   :labels="{ checked: 'On', unchecked: 'Off' }"/>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
    import ToggleButton from 'vue-js-toggle-button/src/Button.vue';

    import UserSkillsService from '@/userSkills/service/UserSkillsService';
    import SkillsSubjectSkillRow from '@/userSkills/skill/progress/SkillsRow.vue';
    import NoDataYet from '@/common/utilities/NoDataYet.vue';
    import SkillsSpinner from '@/common/utilities/SkillsSpinner.vue';

    export default {
        components: {
            NoDataYet,
            SkillsSubjectSkillRow,
            ToggleButton,
            SkillsSpinner,
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
            type: {
                type: String,
                default: 'subject',
            },
        },
        data() {
            return {
                loading: false,
                showDescriptionsInternal: false,
                hasSkills: false,
                descriptionsLoaded: false,
                skillsInternal: [],
            };
        },
        mounted() {
            this.showDescriptionsInternal = this.showDescriptions;
            this.skillsInternal = this.subject.skills.map(item => Object.assign({}, item));
        },
        methods: {
            onDetailsToggle() {
                if (!this.descriptionsLoaded) {
                    this.loading = true;
                    UserSkillsService.getDescriptions(this.subject.subjectId ? this.subject.subjectId : this.subject.badgeId, this.type)
                        .then((res) => {
                            this.descriptions = res;
                            res.forEach((desc) => {
                                const foundSkill = this.skillsInternal.find(skill => desc.skillId === skill.skillId);
                                if (foundSkill) {
                                    foundSkill.description = desc;
                                }
                            });
                            this.descriptionsLoaded = true;
                        })
                        .finally(() => {
                            this.loading = false;
                        });
                }
            },
        },
    };
</script>

<style scoped>
</style>
