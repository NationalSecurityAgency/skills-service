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
  <b-modal :id="firstSkillId" size="lg" :title="`Reuse Skills in this Project`" v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="cancel"
           aria-label="'Reuse Skills in this project'">
    <skills-spinner :is-loading="loadingData"/>

    <div v-if="!loadingData">
      <div id="step1" v-if="!selectedDestination">
        <b-avatar><b>1</b></b-avatar>
        Select Destination:
        <b-list-group class="mt-2">
          <b-list-group-item v-for="dest in destinations.currentPage"
                             :key="`${dest.subjectId}-${dest.groupId}`">
            <div class="row">
              <div class="col">
                <div v-if="!dest.groupId">
                  <span class="font-italic">Subject:</span>
                  <span class="text-primary ml-2 font-weight-bold">{{ dest.subjectName }}</span>
                </div>
                <div v-if="dest.groupId">
                  <div>
                    <span class="font-italic">Group:</span>
                    <span class="text-primary ml-2 font-weight-bold">{{ dest.groupName }}</span>
                  </div>
                  <div>
                    <span class="font-italic">In subject:</span> {{ dest.subjectName }}
                  </div>
                </div>
              </div>
              <div class="col-auto">
                <b-button size="sm" class="float-right text-uppercase" variant="info"
                          @click="selectDestination(dest)">
                  <i class="fas fa-check-circle"/> Select
                </b-button>
              </div>
            </div>
          </b-list-group-item>
        </b-list-group>

        <div class="row align-items-center">
          <div class="col-md text-center text-md-left">
            <!--            <span class="text-muted">Total Rows:</span> <strong data-cy="">{{ destinations.all.length }}</strong>-->
          </div>
          <div class="col-md my-3 my-md-0 pt-2">
            <b-pagination
              v-model="destinations.currentPageNum"
              @change="updateDestinationPage"
              :total-rows="destinations.all.length"
              :per-page="destinations.perPageNum"
              aria-controls="Page Controls for skill reuse destination"
            ></b-pagination>
          </div>
          <div class="col-md text-center text-md-right">
          </div>
        </div>

      </div>

      <div id="step2" v-if="selectedDestination && !state.reUseComplete">
        <div>
          <b-avatar><b>2</b></b-avatar>
          Preview:
        </div>
        <b-card class="mt-2">
          <div v-if="skillsForReuse.available.length > 0">
            <b-badge variant="info">{{ skillsForReuse.available.length }}</b-badge>
            skill{{ plural(skillsForReuse.available) }} will be reused in the
            <span class="text-primary font-weight-bold">{{ selectedDestination.name }}</span>
            subject.
            <div>
              <b-badge variant="warning">{{ skillsForReuse.alreadyExist.length }}</b-badge>
              selected skill{{ plural(skillsForReuse.alreadyExist) }} <span
              class="text-primary font-weight-bold">already</span> been reused!
            </div>
          </div>
          <div v-else>
            <i class="fas fa-exclamation-triangle text-warning mr-2"/>
            <span class="text-warning font-weight-bold">All</span> of the selected skills already
            been reused in the <span
            class="text-primary font-weight-bold">{{ selectedDestination.name }}</span> subject.
            Please cancel and select different skills.
          </div>
        </b-card>
      </div>

      <div id="step3" v-if="state.reUseComplete">
        <div>
          <b-avatar><b>3</b></b-avatar>
          Acknowledgement:
        </div>
        <b-card class="mt-2">
          <span class="text-success">Successfully</span> reused
          <b-badge variant="info">{{ skillsForReuse.available.length }}</b-badge>
          skill{{ plural(skillsForReuse.available) }}.
        </b-card>
      </div>
    </div>

    <div slot="modal-footer" class="w-100">
      <b-button v-if="!state.reUseComplete" variant="success" size="sm" class="float-right"
                @click="initiateReuse"
                :disabled="!selectedDestination || state.reUseInProgress || (skillsForReuse.available && skillsForReuse.available.length === 0)"
                data-cy="reuseButton">
        Reuse
      </b-button>
      <b-button v-if="!state.reUseComplete" variant="secondary" size="sm" class="float-right mr-2"
                @click="cancel"
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

  export default {
    name: 'ReuseSkillsModal',
    components: { SkillsSpinner },
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
        loadingData: true,
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
        },
        skillsForReuse: {
          available: [],
          alreadyExist: [],
          allAlreadyExist: [],
        },
      };
    },
    mounted() {
      this.loadSubjects();
    },
    watch: {
      show(newValue) {
        this.$emit('input', newValue);
      },
    },
    methods: {
      cancel(e) {
        this.show = false;
        this.publishHidden(e, true);
      },
      close(e) {
        this.show = false;
        this.publishHidden(e, false);
      },
      publishHidden(e, cancelled) {
        this.$emit('hidden', {
          ...e,
          cancelled,
        });
      },
      loadSubjects() {
        SkillsService.getReuseDestinationsForASkill(this.$route.params.projectId, this.skills[0].skillId)
          .then((res) => {
            this.destinations.all = res;
            this.updateDestinationPage(this.destinations.currentPageNum);
          })
          .finally(() => {
            this.loadingData = false;
          });
      },
      updateDestinationPage(pageNum) {
        const totalItemsNum = this.destinations.all.length;
        const startIndex = Math.max(0, this.destinations.perPageNum * pageNum - this.destinations.perPageNum);
        const endIndex = Math.min(this.destinations.perPageNum * pageNum, totalItemsNum);
        this.destinations.currentPageNum = pageNum;
        console.log(`currentPageNum: ${pageNum}`);
        console.log(`totalItemsNum: ${totalItemsNum}`);
        console.log(`startIndex: ${startIndex}`);
        console.log(`endIndex: ${endIndex}`);
        this.destinations.currentPage = this.destinations.all.slice(startIndex, endIndex);
      },
      initiateReuse() {
        this.state.reUseInProgress = true;
        const skillIds = this.skillsForReuse.available.map((sk) => sk.skillId);
        SkillsService.reuseSkillInAnotherSubject(this.$route.params.projectId, skillIds, this.selectedDestination.subjectId)
          .then(() => {
            this.state.reUseInProgress = false;
            this.state.reUseComplete = true;
            this.$emit('reused', skillIds);
          });
      },
      selectDestination(selection) {
        this.loadingData = true;
        this.selectedDestination = selection;
        SkillsService.getReusedSkills(this.$route.params.projectId, this.selectedDestination.subjectId)
          .then((res) => {
            this.skillsForReuse.allAlreadyExist = res;
            this.skillsForReuse.alreadyExist = this.skills.filter((skill) => res.find((e) => e.name === skill.name));
            this.skillsForReuse.available = this.skills.filter((skill) => !res.find((e) => e.name === skill.name));
          })
          .finally(() => {
            this.loadingData = false;
          });
      },
      plural(arr) {
        return arr && arr.length > 1 ? 's' : '';
      },
    },
  };
</script>

<style scoped>

</style>
