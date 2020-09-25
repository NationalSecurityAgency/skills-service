/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
    <div class="card mt-2">
        <div class="card-header float-left">
            <div class="row">
                <div class="col">
                    <h6 class="card-title mb-0 float-left">Skills</h6>
                </div>
                <div class="col text-right" v-if="skillsInternal && skillsInternal.length > 0">
                    <span class="text-muted pr-1">Skill Details:</span>
                    <toggle-button class="" v-model="showDescriptionsInternal" @change="onDetailsToggle"
                                   :labels="{ checked: 'On', unchecked: 'Off' }" data-cy="toggleSkillDetails"/>
                </div>
            </div>
        </div>
        <div class="card-body">
            <skills-spinner :loading="loading"/>
            <div v-if="!loading">
                <div v-if="skillsInternal && skillsInternal.length > 0">
                    <skills-subject-skill-row v-for="(skill, index) in skillsInternal" :key="`unique-skill-${index}`"
                                          :skill="skill" :show-description="showDescriptionsInternal"/>
                </div>
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
            </div>
        </div>
    </div>
</template>

<script>
  import ToggleButton from 'vue-js-toggle-button/src/Button';

  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillsSubjectSkillRow from '@/userSkills/skill/progress/SkillsRow';
  import NoDataYet from '@/common/utilities/NoDataYet';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';

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
      this.skillsInternal = this.subject.skills.map((item) => ({ ...item }));
    },
    methods: {
      onDetailsToggle() {
        if (!this.descriptionsLoaded) {
          this.loading = true;
          UserSkillsService.getDescriptions(this.subject.subjectId ? this.subject.subjectId : this.subject.badgeId, this.type)
            .then((res) => {
              this.descriptions = res;
              res.forEach((desc) => {
                const foundSkill = this.skillsInternal.find((skill) => desc.skillId === skill.skillId);
                if (foundSkill) {
                  foundSkill.description = desc;
                  foundSkill.achievedOn = desc.achievedOn;
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
