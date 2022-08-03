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
    <div class="card mt-2" data-cy="skillsProgressList" v-if="(skillsInternal.length > 0 || searchString || showNoDataMsg)">
        <div class="card-header float-left">
            <div class="row" v-if="skillsInternalOrig && skillsInternalOrig.length > 0">
                <div class="col-md-auto text-left pr-md-0">
                  <div class="d-flex">
                    <b-form-input @input="searchSkills" style="padding-right: 2.3rem;"
                                  v-model="searchString"
                                  :placeholder="`Search ${this.skillDisplayName.toLowerCase()}s`"
                                  :aria-label="`Search ${this.skillDisplayName}s`"
                                  data-cy="skillsSearchInput"></b-form-input>
                    <b-button v-if="searchString && searchString.length > 0" @click="clearSearch"
                              class="position-absolute skills-theme-btn" variant="outline-info" style="right: 0rem;"
                              data-cy="clearSkillsSearchInput">
                      <i class="fas fa-times"></i>
                      <span class="sr-only">clear search</span>
                    </b-button>
                  </div>
                </div>
                <div class="col-md text-left my-2 my-md-0 ml-md-0 pl-md-0">
                  <skills-filter :counts="metaCounts" :filters="filters" @filter-selected="filterSkills" @clear-filter="clearFilters"/>
                </div>
                <div class="col-md-auto text-right skill-details-toggle" data-cy="skillDetailsToggle">
                    <span class="text-muted pr-1">{{ skillDisplayName }} Details:</span>
                    <toggle-button class="" v-model="showDescriptionsInternal" @change="onDetailsToggle"
                                   :color="{ checked: '#007c49', unchecked: '#6b6b6b' }"
                                   :aria-label="`Show ${this.skillDisplayName} Details`"
                                   :labels="{ checked: 'On', unchecked: 'Off' }" data-cy="toggleSkillDetails"/>
                </div>
            </div>
        </div>
        <div class="card-body p-0">
            <skills-spinner :loading="loading"/>
            <div v-if="!loading">
                <div v-if="skillsInternal && skillsInternal.length > 0">
                  <div v-for="(skill, index) in skillsInternal"
                       :key="`skill-${skill.skillId}`"
                       class="skills-theme-bottom-border-with-background-color"
                       :class="{
                         'separator-border-thick' : showDescriptionsInternal,
                         'border-bottom' : (index + 1) !== skillsInternal.length
                       }"
                  >
                    <div class="p-3 pt-4">
                      <skill-progress2
                          :id="`skill-${skill.skillId}`"
                          :ref="`skillProgress${skill.skillId}`"
                          :skill="skill"
                          :subjectId="subject.subjectId"
                          :badgeId="subject.badgeId"
                          :type="type"
                          :enable-drill-down="true"
                          :show-description="showDescriptionsInternal"
                          :data-cy="`skillProgress_index-${index}`"
                          @points-earned="onPointsEarned"
                          :child-skill-highlight-string="searchString"
                      />
                    </div>
                  </div>
                </div>
                <no-data-yet v-if="!(skillsInternal && skillsInternal.length > 0) && searchString" class="my-5"
                             icon="fas fa-search-minus fa-5x" title="No results"
                             :sub-title="`Please refine [${searchString}] search${(this.filterId) ? ' and/or clear the selected filter' : ''}`"/>

                <no-data-yet v-if="!(skillsInternalOrig && skillsInternalOrig.length > 0) && showNoDataMsg" class="my-5"
                             :title="`${this.skillDisplayName}s have not been added yet.`"
                             :sub-title="`Please contact this ${this.projectDisplayName.toLowerCase()}'s administrator.`"/>
            </div>
        </div>
    </div>
</template>

<script>
  import ToggleButton from 'vue-js-toggle-button/src/Button';
  import NoDataYet from '@/common-components/utilities/NoDataYet';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';
  import SkillsSpinner from '@/common/utilities/SkillsSpinner';
  import SkillsFilter from '@/common-components/utilities/ListFilterMenu';
  import StringHighlighter from '@/common-components/utilities/StringHighlighter';
  import SkillProgress2 from './SkillProgress2';
  import SkillEnricherUtil from '../../utils/SkillEnricherUtil';

  const updateSkillForLoadedDescription = (skills, desc) => {
    let foundSkill = null;
    for (let i = 0; i < skills.length; i += 1) {
      const skill = skills[i];
      if (desc.skillId === skill.skillId) {
        foundSkill = skill;
      } else if (skill.isSkillsGroupType) {
        foundSkill = skill.children.find((child) => desc.skillId === child.skillId);
      }
      if (foundSkill) {
        break;
      }
    }

    if (foundSkill) {
      foundSkill.description = desc;
      foundSkill.achievedOn = desc.achievedOn;
      foundSkill.selfReporting = desc.selfReporting;
    }
  };

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
      projectId: {
        type: String,
        default: null,
        required: false,
      },
      showNoDataMsg: {
        type: Boolean,
        default: true,
        required: false,
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
        filters: [
          {
            icon: 'fas fa-battery-empty',
            id: 'withoutProgress',
            html: 'Skills <b>without</b> progress',
            count: 0,
          },
          {
            icon: 'far fa-calendar-check',
            id: 'withPointsToday',
            html: 'Skills with points earned <b>today</b>',
            count: 0,
          },
          {
            icon: 'far fa-check-circle',
            id: 'complete',
            html: '<b>Completed</b> skills',
            count: 0,
          },
          {
            icon: 'fas fa-laptop',
            id: 'selfReported',
            html: '<b>Self</b> Reported Skills',
            count: 0,
          },
          {
            icon: 'fas fa-running',
            id: 'inProgress',
            html: 'Skills <b>in progress</b>',
            count: 0,
          },
        ],
      };
    },
    mounted() {
      const theSubject = this.subject;
      this.showDescriptionsInternal = this.showDescriptions;
      let filter = () => true;
      if (this.projectId) {
        filter = (s) => s.projectId === this.projectId;
      }
      this.skillsInternal = this.subject.skills.filter(filter).map((item) => {
        const isSkillsGroupType = item.type === 'SkillsGroup';
        if (isSkillsGroupType) {
          // eslint-disable-next-line no-param-reassign
          item.children = item.children.map((child) => ({ ...child, groupId: item.skillId, isSkillType: true }));
        }
        const res = {
          ...item, subject: theSubject, isSkillsGroupType, isSkillType: !isSkillsGroupType,
        };

        this.updateMetaCountsForSkillRes(res);
        return res;
      });

      this.skillsInternalOrig = this.skillsInternal.map((item) => ({ ...item, children: item.children?.map((child) => ({ ...child })) }));
    },
    methods: {
      updateMetaCountsForSkillRes(skillRes) {
        if (skillRes.isSkillsGroupType) {
          skillRes.children.forEach((childItem) => {
            this.updateMetaCounts(childItem.meta);
          });
        } else {
          this.updateMetaCounts(skillRes.meta);
        }
      },
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
                updateSkillForLoadedDescription(this.skillsInternal, desc);
                updateSkillForLoadedDescription(this.skillsInternalOrig, desc);
              });
              this.descriptionsLoaded = true;
            })
            .finally(() => {
              this.loading = false;
            });
        }
      },
      onPointsEarned(pts, skillId, childSkillId = null) {
        // childSkillId is only provided for SkillsGroup skills
        if (childSkillId) {
          SkillEnricherUtil.updateSkillPtsUnderSkillGroup(this.skillsInternal, pts, skillId, childSkillId);
          SkillEnricherUtil.updateSkillPtsUnderSkillGroup(this.skillsInternalOrig, pts, skillId, childSkillId);
        } else {
          SkillEnricherUtil.updateSkillPtsInList(this.skillsInternalOrig, pts, skillId);
          SkillEnricherUtil.updateSkillPtsInList(this.skillsInternal, pts, skillId);
        }

        const event = { skillId, pointsEarned: pts };
        if (this.type !== 'badge') {
          event.subjectId = this.subject.subjectId;
        } else if (this.type === 'badge') {
          event.badgeId = this.subject.badgeId;
        }
        this.$emit('points-earned', event);
      },
      filterSkills(filterId) {
        this.filterId = filterId.id;
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

          // groups are treated as a single unit (group and child skills shown OR the entire group is removed)
          // group is shown when either a group name matches OR any of the skill names match the search string
          const foundItems = resultSkills.filter((item) => {
            if (item.isSkillsGroupType) {
              // find at least 1 item that matches the search string
              const foundChild = item.children.find((childItem) => childItem.skill?.trim()?.toLowerCase().includes(searchStrNormalized));
              if (foundChild) {
                return true;
              }
            }
            return item.skill?.trim()?.toLowerCase().includes(searchStrNormalized);
          }).map((item) => ({ ...item, children: item.children?.map((child) => ({ ...child })) }));

          resultSkills = foundItems.map((item) => {
            const skillHtml = StringHighlighter.highlight(item.skill, searchStrNormalized);
            return skillHtml ? ({ ...item, skillHtml }) : item;
          });
        }

        if (resultSkills && this.filterId && this.filterId.length > 0) {
          const filteredRes = [];
          resultSkills.forEach((item) => {
            if (item.isSkillsGroupType) {
              const copyItem = ({ ...item });
              copyItem.children = copyItem.children.filter((childItem) => childItem.meta[this.filterId] === true);
              if (copyItem.children && copyItem.children.length > 0) {
                filteredRes.push(copyItem);
              }
            } else if (item.meta[this.filterId] === true) {
              filteredRes.push(item);
            }
          });
          resultSkills = filteredRes;
        }
        this.skillsInternal = resultSkills;
      },
    },
  };
</script>

<style scoped>

.skill-details-toggle > label {
  border: 2px solid transparent;
}

.vue-js-switch:focus-within {
  color: #495057 !important;
  background-color: #fff !important;
  border: 2px solid #80bdff !important;
  border-radius: 11px;
  box-shadow: 1em 1em 1em rgba(0, 0, 0, 0.4) inset, 0 0 2em rgba(128, 189, 255, 0.8) !important;
  outline: none;
}

.separator-border-thick {
  /*border-bottom-color: #f7f7f7 !important;*/
  border-bottom-width: 12px !important;
}
</style>
