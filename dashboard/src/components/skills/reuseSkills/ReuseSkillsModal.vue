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
  <b-modal :id="firstSkillId" size="md" :title="`Reuse Skills in this Project`" v-model="show"
           :no-close-on-backdrop="true" :centered="true"
           header-bg-variant="info" header-text-variant="light" no-fade role="dialog" @hide="cancel"
           aria-label="'Reuse Skills in this project'">
    <skills-spinner :is-loading="loadingData"/>

    <div v-if="!loadingData">
      <div id="step1" v-if="!selectedDestination">
        <b-avatar><b>1</b></b-avatar>
        Select Destination:
        <b-list-group class="mt-2">
          <b-list-group-item v-for="dest in destinations" :key="dest.subjectId">
            <span class="font-italic">Subject:</span><span
            class="text-primary ml-2 font-weight-bold">{{ dest.name }}</span>
            <b-button size="sm" class="float-right text-uppercase" variant="info"
                      @click="selectDestination(dest)">
              <i class="fas fa-check-circle"/> Select
            </b-button>
          </b-list-group-item>
        </b-list-group>
      </div>

      <div id="step2" v-if="selectedDestination && !state.reUseComplete">
        <div>
          <b-avatar><b>2</b></b-avatar>
          Preview:
        </div>
        <b-card class="mt-2">
          <b-badge variant="info">{{ skills.length }}</b-badge>
          skill{{ plural }} will be reused in the
          <span class="text-primary font-weight-bold">{{ selectedDestination.name }}</span> subject.
        </b-card>
      </div>

      <div id="step3" v-if="state.reUseComplete">
        <div>
          <b-avatar><b>3</b></b-avatar>
          Acknowledgement:
        </div>
        <b-card class="mt-2">
          <span class="text-success">Successfully</span> reused
          <b-badge variant="info">{{ skills.length }}</b-badge>
          skill{{ plural }}.
        </b-card>
      </div>
    </div>

    <div slot="modal-footer" class="w-100">
      <b-button v-if="!state.reUseComplete" variant="success" size="sm" class="float-right"
                @click="initiateReuse"
                :disabled="!selectedDestination || state.reUseInProgress"
                data-cy="reuseButton">
        Reuse
      </b-button>
      <b-button v-if="!state.reUseComplete" variant="secondary" size="sm" class="float-right mr-2"
                @click="cancel"
                data-cy="closeButton">
        Cancel
      </b-button>

      <b-button v-if="state.reUseComplete" variant="success" size="sm" class="float-right mr-2"
                @click="cancel"
                data-cy="okButton">
        OK
      </b-button>
    </div>
  </b-modal>
</template>

<script>
  import SubjectsService from '@/components/subjects/SubjectsService';
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
        destinations: [],
        selectedDestination: null,
        state: {
          reUseInProgress: false,
          reUseComplete: false,
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
      publishHidden(e, cancelled) {
        this.$emit('hidden', {
          ...e,
          cancelled
        });
      },
      loadSubjects() {
        SubjectsService.getSubjects(this.$route.params.projectId)
          .then((subjects) => {
            const otherSubjects = subjects.filter((subj) => subj.subjectId !== this.$route.params.subjectId);
            this.destinations = otherSubjects;
          })
          .finally(() => {
            this.loadingData = false;
          });
      },
      initiateReuse() {
        this.state.reUseInProgress = true;
        const skillIds = this.skills.map((sk) => sk.skillId);
        SkillsService.reuseSkillInAnotherSubject(this.$route.params.projectId, skillIds, this.selectedDestination.subjectId)
          .then(() => {
            this.state.reUseInProgress = false;
            this.state.reUseComplete = true;
          });
      },
      selectDestination(selection) {
        this.selectedDestination = selection;
      },
    },
    computed: {
      plural() {
        return this.skills.length > 1 ? 's' : '';
      },
    },
  };
</script>

<style scoped>

</style>
