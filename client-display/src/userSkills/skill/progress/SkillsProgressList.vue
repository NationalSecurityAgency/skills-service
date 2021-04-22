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
            <div class="row" v-if="skillsInternalOrig && skillsInternalOrig.length > 0">
                <div class="col-md-auto text-left pr-md-0">
                  <div class="d-flex">
                    <b-form-input @input="searchSkills" style="padding-right: 2.3rem;"
                                  v-model="searchString"
                                  placeholder="Search skills"
                                  aria-label="Search skills"
                                  data-cy="skillsSearchInput"></b-form-input>
                    <b-button v-if="searchString && searchString.length > 0" @click="clearSearch"
                              class="position-absolute" variant="outline-info" style="right: 0rem;"
                              data-cy="clearSkillsSearchInput">
                      <i class="fas fa-times"></i>
                    </b-button>
                  </div>
                </div>
                <div class="col-md text-left my-2 my-md-0 ml-md-0 pl-md-0">
                  <skills-filter :counts="metaCounts" @filter-selected="filterSkills" @clear-filter="clearFilters"/>
                </div>
                <div class="col-md-auto text-right" >
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
                          @points-earned="onPointsEarned"
                      />
                    </div>
                  </div>
                </div>
                <no-data-yet v-if="!(skillsInternal && skillsInternal.length > 0) && searchString" class="my-5"
                             icon="fas fa-search-minus fa-5x"
                           title="No results" :sub-title="`Please refine [${searchString}] search${(this.filterId) ? ' and/or clear the selected filter' : ''}`"/>

                <no-data-yet v-if="!(skillsInternalOrig && skillsInternalOrig.length > 0)" class="my-5"
                        title="Skills have not been added yet." sub-title="Please contact this project's administrator."/>
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
  import SkillsFilter from './SkillsFilter';
  import SkillEnricherUtil from '../../utils/SkillEnricherUtil';

  export default {
    components: {
      SkillsFilter,
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
      type: {
        type: String,
        default: 'subject',
      },
    },
    data() {
      return {
        searchString: '',
        filterId: '',
        metaCounts: {
          complete: 0,
          selfReported: 0,
          withPointsToday: 0,
          withoutProgress: 0,
          inProgress: 0,
        },
        loading: false,
        showDescriptionsInternal: false,
        hasSkills: false,
        descriptionsLoaded: false,
        skillsInternal: [],
        skillsInternalOrig: [],
      };
    },
    mounted() {
      this.showDescriptionsInternal = this.showDescriptions;
      this.skillsInternal = this.subject.skills.map((item) => {
        this.updateMetaCounts(item.meta);
        return { ...item };
      });
      this.skillsInternalOrig = this.skillsInternal.map((item) => ({ ...item }));
    },
    methods: {
      updateMetaCounts(meta) {
        if (meta.complete) {
          this.metaCounts.complete += 1;
        }
        if (meta.selfReported) {
          this.metaCounts.selfReported += 1;
        }
        if (meta.withPointsToday) {
          this.metaCounts.withPointsToday += 1;
        }
        if (meta.withoutProgress) {
          this.metaCounts.withoutProgress += 1;
        }
        if (meta.inProgress) {
          this.metaCounts.inProgress += 1;
        }
      },
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
      onPointsEarned(pts, skillId) {
        const updateSkill = (skills) => {
          const index = skills.findIndex((item) => item.skillId === skillId);
          const skill = skills[index];
          const updatedSkill = SkillEnricherUtil.addPts(skill, pts);
          skills.splice(index, 1, updatedSkill);
        };

        updateSkill(this.skillsInternalOrig);
        updateSkill(this.skillsInternal);
      },
      filterSkills(filterId) {
        this.filterId = filterId;
        this.searchAndFilterSkills();
      },
      clearFilters() {
        this.filterId = '';
        this.searchAndFilterSkills();
      },
      searchSkills(searchString) {
        this.searchString = searchString;
        this.searchAndFilterSkills();
      },
      clearSearch() {
        this.searchString = '';
        this.searchAndFilterSkills();
      },
      searchAndFilterSkills() {
        let resultSkills = this.skillsInternalOrig.map((item) => ({ ...item }));
        if (this.searchString && this.searchString.trim().length > 0) {
          const searchStrNormalized = this.searchString.trim().toLowerCase();
          const foundItems = resultSkills.filter((item) => item.skill?.trim()?.toLowerCase().includes(searchStrNormalized));
          resultSkills = foundItems.map((item) => {
            const name = item.skill;
            const index = name.toLowerCase().indexOf(searchStrNormalized);
            const skillHtml = `${name.substring(0, index)}<mark>${name.substring(index, index + searchStrNormalized.length)}</mark>${name.substring(index + searchStrNormalized.length)}`;
            return { skillHtml, ...item };
          });
        }

        if (this.filterId && this.filterId.length > 0) {
          resultSkills = resultSkills.filter((item) => item.meta[this.filterId] === true);
        }
        this.skillsInternal = resultSkills;
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
