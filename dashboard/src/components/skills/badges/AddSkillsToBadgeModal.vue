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
  <b-modal :id="firstSkillId" size="lg" title="Add Skills to Badge"
           v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="cancel"
           aria-label="'Add skills to a badge in this project'">
    <skills-spinner :is-loading="isLoading"/>

    <div v-if="!isLoading" data-cy="addSkillsToBadgeModalContent">
      <div v-if="learningPathViolationErr.show" class="alert alert-danger mx-3" data-cy="learningPathErrMsg">
        <i class="fas fa-exclamation-triangle" aria-hidden="true" />
        Failed to add <b>{{ learningPathViolationErr.skillName }}</b> skill to the badge.
        Adding this skill would result in a <b>circular/infinite learning path</b>.
        Please visit project's <b-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path</b-link> page to review.
      </div>

      <div id="step1" v-if="!selectedDestination && !state.addSkillsToBadgeInProgress"
           data-cy="addSkillsToBadgeModalStep1">
        <div v-if="destinations.all && destinations.all.length > 0">
          <b-avatar><b>1</b></b-avatar>
          Select Destination:
          <b-list-group class="mt-2" data-cy="destinationList">
            <b-list-group-item v-for="(dest, index) in destinations.currentPage"
                               :key="`${dest.badgeId}`"
                               :data-cy="`destItem-${index}`">
              <div class="row">
                <div class="col">
                  <div class="row">
                    <div class="col-auto m-0 px-2 text-primary" style="font-size: 1.5rem">
                      <i class="fas fa-award"/>
                    </div>
                    <div class="col px-0 py-1">
                      <div>
                        <span class="font-italic">Badge:</span>
                        <span class="text-primary ml-2 font-weight-bold">{{
                            dest.name
                          }}</span>
                      </div>
                    </div>
                  </div>
                </div>
                <div class="col-auto">
                  <b-button size="sm" class="float-right text-uppercase" variant="info"
                            @click="selectDestination(dest)"
                            :data-cy="`selectDest_${dest.badgeId}`">
                    <i class="fas fa-check-circle"/> Select
                  </b-button>
                </div>
              </div>
            </b-list-group-item>
          </b-list-group>

          <div class="row align-items-center"
               v-if="destinations.all.length > destinations.currentPage.length">
            <div class="col-md text-center text-md-left">
            </div>
            <div class="col-md my-3 my-md-0 pt-2">
              <b-pagination
                v-model="destinations.currentPageNum"
                @change="updateDestinationPage"
                :total-rows="destinations.all.length"
                :per-page="destinations.perPageNum"
                aria-controls="Page Controls for destination badge selection"
                data-cy="destListPagingControl"
              ></b-pagination>
            </div>
            <div class="col-md text-center text-md-right">
            </div>
          </div>
        </div>
        <div v-else>
          <no-content2 title="No Badges Available"
                       message="There are no Badges available. A badge must be created before adding skills to it."/>
        </div>
      </div>

      <div id="step2" v-if="selectedDestination && !state.addSkillsToBadgeComplete && !state.addSkillsToBadgeInProgress"
           data-cy="addSkillsToBadgeModalStep2">
        <div v-if="!state.errorOnSave">
          <b-avatar><b>2</b></b-avatar>
          Preview:
        </div>
        <b-card class="mt-2">
          <div v-if="skillsForBadge.skillsWithLearningPathViolations.length === 0 && skillsForBadge.available.length > 0 && !state.errorOnSave">
            <b-badge variant="info">{{ skillsForBadge.available.length }}</b-badge>
            skill{{ plural(skillsForBadge.available) }} will be added to the
            <span>
              <span class="text-primary font-weight-bold">[{{
                  selectedDestination.name
                }}]</span>
              badge.
            </span>
          </div>
          <div v-else>
            <i class="fas fa-exclamation-triangle text-warning mr-2"/>
            Selected skills can NOT be added to the
            <span
              class="text-primary font-weight-bold">{{ selectedDestination.name }} </span> badge.
            Please cancel and select different skills.
          </div>
          <div v-if="skillsForBadge.skillsWithLearningPathViolations.length === 0 && skillsForBadge.alreadyExist.length > 0">
            <b-badge variant="warning">{{ skillsForBadge.alreadyExist.length }}</b-badge>
            selected skill{{ pluralWithHave(skillsForBadge.alreadyExist) }} <span
            class="text-primary font-weight-bold">already</span> been added to that badge!
          </div>
          <div v-for="(skill) in skillsForBadge.skillsWithLearningPathViolations" :key="skill.skillId" class="alert alert-danger mx-3" :data-cy="`learningPathErrMsg-${skill.skillId}`">
            <i class="fas fa-exclamation-triangle" aria-hidden="true" />
            Unable to add <b>{{ skill.name }}</b> skill to the badge.
            Adding this skill would result in a <b>circular/infinite learning path</b>.
            Please visit project's <b-link :to="{ name: 'FullDependencyGraph' }" data-cy="learningPathLink">Learning Path</b-link> page to review.
          </div>
        </b-card>
      </div>

      <div id="step3" v-if="state.addSkillsToBadgeComplete" data-cy="addSkillsToBadgeModalStep3">
        <div>
          <b-avatar><b>3</b></b-avatar>
          Acknowledgement:
        </div>
        <b-card class="mt-2">
          <span class="text-success">Successfully</span> added
          <b-badge variant="info">{{ skillsForBadge.available.length }}</b-badge>
          skill{{ plural(skillsForBadge.available) }} to the <span>
              <span class="text-primary font-weight-bold">[{{
                  selectedDestination.name
                }}]</span>
              badge.
            </span>
        </b-card>
      </div>
    </div>

    <div slot="modal-footer" class="w-100">
      <b-button v-if="!state.addSkillsToBadgeComplete && !state.errorOnSave" variant="success" size="sm" class="float-right"
                @click="addSkillsToBadge"
                :disabled="!selectedDestination || state.addSkillsToBadgeInProgress || (skillsForBadge.available && skillsForBadge.available.length === 0) || skillsForBadge.skillsWithLearningPathViolations.length > 0"
                data-cy="addSkillsToBadgeButton">
        Add
      </b-button>
      <b-button v-if="!state.addSkillsToBadgeComplete" variant="secondary" size="sm" class="float-right mr-2"
                @click="cancel"
                :disabled="state.addSkillsToBadgeInProgress"
                data-cy="closeButton">
        Cancel
      </b-button>

      <b-button v-if="state.addSkillsToBadgeComplete" variant="success" size="sm" class="float-right mr-2"
                @click="close"
                data-cy="okButton">
        OK
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import { SkillsReporter } from '@skilltree/skills-client-vue';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import SkillsService from '@/components/skills/SkillsService';
  import BadgesService from '@/components/badges/BadgesService';
  import NoContent2 from '@/components/utils/NoContent2';
  import NavigationErrorMixin from '@/components/utils/NavigationErrorMixin';

  export default {
    name: 'AddSkillsToBadgeModal',
    mixins: [NavigationErrorMixin],
    components: {
      NoContent2,
      SkillsSpinner,
    },
    props: {
      skills: {
        type: Array,
        required: true,
      },
      value: {
        type: Boolean,
        required: true,
      },
    },
    data() {
      return {
        show: this.value,
        loading: {
          badges: true,
          existingBadgeSkills: false,
        },
        firstSkillId: null,
        destinations: {
          all: [],
          currentPage: [],
          perPageNum: 4,
          currentPageNum: 1,
        },
        selectedDestination: null,
        state: {
          addSkillsToBadgeInProgress: false,
          addSkillsToBadgeComplete: false,
          errorOnSave: false,
        },
        skillsForBadge: {
          available: [],
          alreadyExist: [],
          allAlreadyExist: [],
          skillsWithLearningPathViolations: [],
        },
        learningPathViolationErr: {
          show: false,
          skillName: '',
        },
      };
    },
    mounted() {
      this.firstSkillId = this.skills[0].skillId;
      this.loadBadges();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      isLoading() {
        return this.loading.badges || this.loading.existingBadgeSkills;
      },
    },
    methods: {
      cancel(e) {
        this.show = false;
        this.publishHidden(e, true);
      },
      close(e) {
        if (this.state.addSkillsToBadgeComplete) {
          this.$emit('action-success', {
            destination: this.selectedDestination,
            skillsAddedToBadge: this.skillsForBadge.available,
          });
        } else {
          this.publishHidden(e, false);
        }
        this.show = false;
      },
      publishHidden(e, cancelled) {
        this.$emit('hidden', {
          ...e,
          cancelled,
        });
      },
      loadBadges() {
        BadgesService.getBadges(this.$route.params.projectId)
          .then((res) => {
            this.destinations.all = res;
            this.updateDestinationPage(this.destinations.currentPageNum);
          })
          .finally(() => {
            this.loading.badges = false;
          });
      },
      updateDestinationPage(pageNum) {
        const totalItemsNum = this.destinations.all.length;
        const startIndex = Math.max(0, this.destinations.perPageNum * pageNum - this.destinations.perPageNum);
        const perPageNum = totalItemsNum <= this.destinations.perPageNum + 1 ? this.destinations.perPageNum + 1 : this.destinations.perPageNum;
        const endIndex = Math.min(perPageNum * pageNum, totalItemsNum);
        this.destinations.currentPageNum = pageNum;
        this.destinations.currentPage = this.destinations.all ? this.destinations.all.slice(startIndex, endIndex) : [];
      },
      addSkillsToBadge() {
        this.state.addSkillsToBadgeInProgress = true;
        const skillIds = this.skillsForBadge.available.map((sk) => sk.skillId);
        SkillsService.assignSkillsToBadge(this.$route.params.projectId, this.selectedDestination.badgeId, skillIds, false)
          .then(() => {
            this.handleActionCompleting();
          })
          .catch((e) => {
            if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'LearningPathViolation') {
              this.state.addSkillsToBadgeInProgress = false;
              this.learningPathViolationErr.show = true;
              this.learningPathViolationErr.skillName = this.skillsForBadge.available.find((sk) => sk.skillId === e.response.data.skillId)?.name;
              this.state.errorOnSave = true;
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
              this.handlePush({
                name: 'ErrorPage',
                query: { errorMessage },
              });
            }
          });
      },
      handleActionCompleting() {
        this.state.addSkillsToBadgeInProgress = false;
        this.state.addSkillsToBadgeComplete = true;
        SkillsReporter.reportSkill('AssignGemOrBadgeSkills');
      },
      selectDestination(selection) {
        this.loading.existingBadgeSkills = true;
        this.selectedDestination = selection;
        const { projectId } = this.$route.params;
        const { badgeId } = this.selectedDestination;
        SkillsService.getBadgeSkills(projectId, badgeId)
          .then((res) => {
            this.skillsForBadge.allAlreadyExist = res;
            this.skillsForBadge.alreadyExist = this.skills.filter((skill) => res.find((e) => e.skillId === skill.skillId));
            const availableSkills = this.skills.filter((skill) => !res.find((e) => e.skillId === skill.skillId));
            if (availableSkills.length > 0) {
              availableSkills.forEach((skill) => {
                if (!this.learningPathViolationErr.show) {
                  SkillsService.validateDependency(projectId, badgeId, skill.skillId, projectId).then((dependencyRes) => {
                    if (!dependencyRes.possible && dependencyRes.failureType !== 'NotEligible') {
                      this.skillsForBadge.skillsWithLearningPathViolations.push(skill);
                    }
                  });
                }
              });
              this.skillsForBadge.available = availableSkills;
            }
          })
          .finally(() => {
            this.loading.existingBadgeSkills = false;
          });
      },
      plural(arr) {
        return arr && arr.length > 1 ? 's' : '';
      },
      pluralWithHave(arr) {
        return arr && arr.length > 1 ? 's have' : ' has';
      },
    },
  };
</script>

<style scoped>

</style>
