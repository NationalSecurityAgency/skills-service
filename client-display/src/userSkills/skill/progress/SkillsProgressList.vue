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
                    <h3 class="h6 card-title mb-0 float-left">Skills</h3>
                </div>
                <div class="col text-right" v-if="skillsInternal && skillsInternal.length > 0">
                    <span class="text-muted pr-1">Skill Details:</span>
                    <toggle-button class="" v-model="showDescriptionsInternal" @change="onDetailsToggle"
                                   :color="{ checked: '#007c49', unchecked: '#6b6b6b' }"
                                   :labels="{ checked: 'On', unchecked: 'Off' }" data-cy="toggleSkillDetails"/>
                </div>
            </div>
        </div>
        <div class="card-body p-0">
            <skills-spinner :loading="loading"/>
            <div v-if="!loading">
                <div v-if="skillsInternal && skillsInternal.length > 0">
                  <div v-for="(skill, index) in skillsInternal"
                       :key="`unique-skill-old-${index}`"
                       class="skills-theme-bottom-border-with-background-color"
                       :class="{
                         'separator-border-thick' : showDescriptionsInternal,
                         'border-bottom' : (index + 1) !== skillsInternal.length
                       }"
                  >
                    <div class="p-3 pt-4">
                      <skill-progress2
                          :skill="skill"
                          :enable-drill-down="true"
                          :show-description="showDescriptionsInternal"
                          :data-cy="`skillProgress_index-${index}`"
                      />
                    </div>
                  </div>
                </div>
                <no-data-yet v-if="!(skillsInternal && skillsInternal.length > 0)" class="my-2"
                        title="Skills have not been added yet." sub-title="Please contact this project's administrator."/>
            </div>
        </div>

        <div v-if="skillsInternal && skillsInternal.length > 0" class="card-footer skills-page-title-text-color">
            <div class="row">
                <div class="col text-right">
                  <span v-if="helpTipHref">
                    Need Help?
                    <a :href="helpTipHref" target="_blank" rel="noopener" class="">
                      Click here <i class="fas fa-external-link-alt"></i>
                    </a>
                  </span>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
  import ToggleButton from 'vue-js-toggle-button/src/Button';

  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import NoDataYet from '@/common/utilities/NoDataYet';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import SkillProgress2 from './SkillProgress2';

  export default {
    components: {
      SkillProgress2,
      NoDataYet,
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
                  foundSkill.selfReporting = desc.selfReporting;
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
.separator-border-thick {
  /*border-bottom-color: #f7f7f7 !important;*/
  border-bottom-width: 12px !important;
}
</style>
