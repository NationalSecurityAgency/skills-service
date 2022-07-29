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
  <b-modal :id="firstSkillId" size="lg" :title="`${actionName} Skills in this Project`"
           v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="cancel"
           aria-label="'Reuse Skills in this project'">
    <skills-spinner :is-loading="isLoading"/>

    <div v-if="!isLoading" data-cy="reuseModalContent">
      <no-content2 v-if="importFinalizePending" :title="`Cannot ${actionName}`"
                   :message="`Cannot initiate skill ${actionNameLowerCase} while skill finalization is pending.`"/>
      <no-content2 v-if="state.skillsWereMovedOrReusedAlready" title="Please Refresh"
                   message="Skills were moved or reused in another browser tab OR modified by another project administrator. Please refresh the page."/>

      <div v-if="!importFinalizePending && !state.skillsWereMovedOrReusedAlready">
        <div id="step1" v-if="!selectedDestination && !state.reUseInProgress"
             data-cy="reuseSkillsModalStep1">
          <div v-if="destinations.all && destinations.all.length > 0">
            <b-avatar><b>1</b></b-avatar>
            Select Destination:
            <b-list-group class="mt-2" data-cy="destinationList">
              <b-list-group-item v-for="(dest, index) in destinations.currentPage"
                                 :key="`${dest.subjectId}-${dest.groupId}`"
                                 :data-cy="`destItem-${index}`">
                <div class="row">
                  <div class="col">
                    <div class="row">
                      <div class="col-auto m-0 px-2 text-primary" style="font-size: 1.5rem">
                        <i v-if="dest.groupId" class="fas fa-layer-group"/>
                        <i v-else class="fas fa-cubes"/>
                      </div>
                      <div class="col px-0 py-1">
                        <div v-if="!dest.groupId">
                          <span class="font-italic">Subject:</span>
                          <span class="text-primary ml-2 font-weight-bold">{{
                              dest.subjectName
                            }}</span>
                        </div>
                        <div v-if="dest.groupId">
                          <div>
                            <span class="font-italic">Group:</span>
                            <span class="text-primary ml-2 font-weight-bold">{{
                                dest.groupName
                              }}</span>
                          </div>
                          <div>
                            <span class="font-italic">In subject:</span> {{ dest.subjectName }}
                          </div>
                        </div>
                      </div>
                    </div>
                  </div>
                  <div class="col-auto">
                    <b-button size="sm" class="float-right text-uppercase" variant="info"
                              @click="selectDestination(dest)"
                              :data-cy="`selectDest_subj${dest.subjectId}${dest.groupId ? dest.groupId : ''}`">
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
                  :aria-controls="`Page Controls for skill ${actionName} destination`"
                  data-cy="destListPagingControl"
                ></b-pagination>
              </div>
              <div class="col-md text-center text-md-right">
              </div>
            </div>
          </div>
          <div v-else>
            <no-content2 title="No Destinations Available"
                         :message="`There are no Subjects or Groups that this skill can be ${actionNameInPast} ${actionDirection}. Please create additional subjects and/or groups if you want to ${actionNameLowerCase} skills.`"/>
          </div>
        </div>

        <div id="step2" v-if="selectedDestination && !state.reUseComplete && !state.reUseInProgress"
             data-cy="reuseSkillsModalStep2">
          <div>
            <b-avatar><b>2</b></b-avatar>
            Preview:
          </div>
          <b-card class="mt-2">
            <div v-if="skillsForReuse.available.length > 0">
              <b-badge variant="info">{{ skillsForReuse.available.length }}</b-badge>
              skill{{ plural(skillsForReuse.available) }} will be {{ actionNameInPast }}
              {{ actionDirection }} the
              <span v-if="selectedDestination.groupName">
                <span class="text-primary font-weight-bold">[{{
                    selectedDestination.groupName
                  }}]</span>
                group.
              </span>
              <span v-else>
                <span class="text-primary font-weight-bold">[{{
                    selectedDestination.subjectName
                  }}]</span>
                subject.
              </span>
            </div>
            <div v-else>
              <i class="fas fa-exclamation-triangle text-warning mr-2"/>
              Selected skills can NOT be {{ actionNameInPast }} {{ actionDirection }} the
              <span v-if="selectedDestination.groupName"><span
                class="text-primary font-weight-bold">{{ selectedDestination.groupName }} </span> group</span>
              <span v-else><span
                class="text-primary font-weight-bold">{{ selectedDestination.subjectName }} </span> subject</span>.
              Please cancel and select different skills.
            </div>
            <div v-if="skillsForReuse.alreadyExist.length > 0">
              <b-badge variant="warning">{{ skillsForReuse.alreadyExist.length }}</b-badge>
              selected skill{{ pluralWithHave(skillsForReuse.alreadyExist) }} <span
              class="text-primary font-weight-bold">already</span> been reused in that <span
              v-if="selectedDestination.groupName">group</span><span v-else>subject</span>!
            </div>
            <div v-if="skillsForReuse.skillsWithDeps.length > 0">
              <b-badge variant="warning">{{ skillsForReuse.skillsWithDeps.length }}</b-badge>
              selected skill{{ pluralWithHave(skillsForReuse.skillsWithDeps) }} other skill
              dependencies, reusing skills with dependencies is not allowed!
            </div>
          </b-card>
        </div>

        <div v-if="state.reUseInProgress">
          <div>
            <b-avatar><b>2A</b></b-avatar>
            In Progress:
          </div>
          <b-card class="mt-2">
            Working very hard to {{ actionName }}
            <b-badge variant="info">{{ skillsForReuse.available.length }}</b-badge>
            skill{{ skillsForReuse.available.length > 1 ? 's' : '' }}. This may take several
            minutes.
            <lengthy-operation-progress-bar name="Finalize" class="mb-3 mt-1"/>
          </b-card>
        </div>

        <div id="step3" v-if="state.reUseComplete" data-cy="reuseSkillsModalStep3">
          <div>
            <b-avatar><b>3</b></b-avatar>
            Acknowledgement:
          </div>
          <b-card class="mt-2">
            <span class="text-success">Successfully</span> {{ actionNameInPast }}
            <b-badge variant="info">{{ skillsForReuse.available.length }}</b-badge>
            skill{{ plural(skillsForReuse.available) }}.
          </b-card>
        </div>
      </div>
    </div>

    <div slot="modal-footer" class="w-100">
      <b-button v-if="!state.reUseComplete" variant="success" size="sm" class="float-right"
                @click="initiateReuse"
                :disabled="!selectedDestination || state.reUseInProgress || (skillsForReuse.available && skillsForReuse.available.length === 0) || state.reUseInProgress"
                data-cy="reuseButton">
        {{ actionName }}
      </b-button>
      <b-button v-if="!state.reUseComplete" variant="secondary" size="sm" class="float-right mr-2"
                @click="cancel"
                :disabled="state.reUseInProgress"
                data-cy="closeButton">
        Cancel
      </b-button>

      <b-button v-if="state.reUseComplete" variant="success" size="sm" class="float-right mr-2"
                @click="close"
                data-cy="okButton">
        OK
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import SkillsSpinner from '@/components/utils/SkillsSpinner';
  import SkillsService from '@/components/skills/SkillsService';
  import LengthyOperationProgressBar from '@/components/utils/LengthyOperationProgressBar';
  import NoContent2 from '@/components/utils/NoContent2';
  import CatalogService from '@/components/skills/catalog/CatalogService';
  import NavigationErrorMixin from '@/components/utils/NavigationErrorMixin';

  export default {
    name: 'ReuseSkillsModal',
    mixins: [NavigationErrorMixin],
    components: {
      NoContent2,
      LengthyOperationProgressBar,
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
      type: {
        type: String,
        required: false,
        default: 'reuse',
        validator(value) {
          return ['reuse', 'move'].includes(value);
        },
      },
    },
    data() {
      return {
        show: this.value,
        isReuseType: this.type === 'reuse',
        isMoveType: this.type === 'move',
        textCustomization: {
          actionName: 'Reuse',
          actionDirection: 'in',
        },
        loading: {
          subjects: true,
          reusedSkills: false,
          finalizationInfo: true,
          dependencyInfo: false,
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
          reUseInProgress: false,
          reUseComplete: false,
          skillsWereMovedOrReusedAlready: false,
        },
        skillsForReuse: {
          available: [],
          alreadyExist: [],
          allAlreadyExist: [],
          skillsWithDeps: [],
        },
        finalizeInfo: {},
      };
    },
    mounted() {
      if (this.type === 'move') {
        this.textCustomization = {
          actionName: 'Move',
          actionDirection: 'to',
        };
      }
      this.loadSubjects();
      this.loadFinalizeInfo();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    computed: {
      isLoading() {
        return this.loading.subjects || this.loading.reusedSkills || this.loading.finalizationInfo || this.loading.dependencyInfo;
      },
      importFinalizePending() {
        return this.finalizeInfo && this.finalizeInfo.numSkillsToFinalize && this.finalizeInfo.numSkillsToFinalize > 0;
      },
      actionNameInPast() {
        return `${this.textCustomization.actionName.toLowerCase()}d`;
      },
      actionName() {
        return this.textCustomization.actionName;
      },
      actionNameLowerCase() {
        return this.textCustomization.actionName.toLowerCase();
      },
      actionDirection() {
        return this.textCustomization.actionDirection;
      },
    },
    methods: {
      cancel(e) {
        this.show = false;
        this.publishHidden(e, true);
      },
      close(e) {
        if (this.state.reUseComplete) {
          this.$emit('reused', {
            destination: this.selectedDestination,
            reusedSkills: this.skillsForReuse.available,
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
      loadSubjects() {
        SkillsService.getSkillInfo(this.$route.params.projectId, this.skills[0].skillId)
          .then((skillInfo) => {
            if (skillInfo.subjectId !== this.$route.params.subjectId) {
              this.state.skillsWereMovedOrReusedAlready = true;
              this.loading.subjects = false;
            } else {
              SkillsService.getReuseDestinationsForASkill(this.$route.params.projectId, this.skills[0].skillId)
                .then((res) => {
                  this.destinations.all = res;
                  this.updateDestinationPage(this.destinations.currentPageNum);
                })
                .finally(() => {
                  this.loading.subjects = false;
                });
            }
          });
      },
      loadFinalizeInfo() {
        CatalogService.getCatalogFinalizeInfo(this.$route.params.projectId)
          .then((res) => {
            this.finalizeInfo = res;
          })
          .finally(() => {
            this.loading.finalizationInfo = false;
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
      initiateReuse() {
        this.state.reUseInProgress = true;
        const skillIds = this.skillsForReuse.available.map((sk) => sk.skillId);
        if (this.isMoveType) {
          SkillsService.moveSkills(this.$route.params.projectId, skillIds, this.selectedDestination.subjectId, this.selectedDestination.groupId, false)
            .then(() => {
              this.handleActionCompleting();
            })
            .catch((e) => {
              if (e.response.data && e.response.data.explanation && e.response.data.explanation.includes('All moved skills must come from the same parent')) {
                this.state.reUseInProgress = false;
                this.state.skillsWereMovedOrReusedAlready = true;
                this.selectedDestination = null;
              } else {
                const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
                this.handlePush({
                  name: 'ErrorPage',
                  query: { errorMessage },
                });
              }
            });
        } else {
          SkillsService.reuseSkillInAnotherSubject(this.$route.params.projectId, skillIds, this.selectedDestination.subjectId, this.selectedDestination.groupId)
            .then(() => {
              this.handleActionCompleting();
            });
        }
      },
      handleActionCompleting() {
        this.state.reUseInProgress = false;
        this.state.reUseComplete = true;
      },
      selectDestination(selection) {
        this.loading.reusedSkills = true;
        this.selectedDestination = selection;
        const parentId = this.selectedDestination.groupId ? this.selectedDestination.groupId : this.selectedDestination.subjectId;
        SkillsService.getReusedSkills(this.$route.params.projectId, parentId)
          .then((res) => {
            this.skillsForReuse.allAlreadyExist = res;
            this.skillsForReuse.alreadyExist = this.skills.filter((skill) => res.find((e) => e.name === skill.name));
            this.skillsForReuse.available = this.skills.filter((skill) => !res.find((e) => e.name === skill.name));
            if (this.skillsForReuse.available.length > 0) {
              this.loadDependencyInfo();
            }
          })
          .finally(() => {
            this.loading.reusedSkills = false;
          });
      },
      loadDependencyInfo() {
        if (this.isReuseType) {
          this.loading.dependencyInfo = true;
          SkillsService.checkSkillsForDeps(this.$route.params.projectId, this.skillsForReuse.available.map((item) => item.skillId))
            .then((res) => {
              const withDeps = res.filter((item) => item.hasDependency);
              this.skillsForReuse.skillsWithDeps = this.skillsForReuse.available.filter((skill) => withDeps.find((e) => e.skillId === skill.skillId));
              this.skillsForReuse.available = this.skillsForReuse.available.filter((skill) => !withDeps.find((e) => e.skillId === skill.skillId));
            })
            .finally(() => {
              this.loading.dependencyInfo = false;
            });
        }
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
