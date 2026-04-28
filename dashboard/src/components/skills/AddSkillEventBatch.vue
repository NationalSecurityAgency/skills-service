/*
Copyright 2026 SkillTree

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
<script setup>
import AddSkillEventForm from "@/components/skills/AddSkillEventForm.vue";
import {ref} from "vue";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import SkillsService from "@/components/skills/SkillsService.js";
import StepList from "primevue/steplist";
import Step from "primevue/step";
import StepPanel from "primevue/steppanel";
import StepPanels from "primevue/steppanels";
import Stepper from 'primevue/stepper'
import Column from "primevue/column";
import dayjs from "dayjs";

const model = defineModel()
const emit = defineEmits([])

const props = defineProps({
  skills: {
    type: Array,
    required: true
  },
  projectId: {
    type: String,
    required: true
  }
})

const usersToAdd = ref([]);
const dateAdded = ref(new Date());
const results = ref([]);

const saveEvents = () => {
  const userIds = usersToAdd.value.map((user) => user.userId);
  const skillIds = props.skills.map((skill) => skill.skillId);

  return SkillsService.saveSkillEventBatch(props.projectId, skillIds, userIds, dateAdded.value.getTime(), true).then((result) => {
    usersToAdd.value = [];
    results.value = result.results;
  })

}

const addUser = (user) => {
  usersToAdd.value.push(user);
}

const closeMe = () => {
  model.value = false;
}
</script>

<template>
  <SkillsDialog
      id="addSkillEventBatch"
      header="Add Skill Events"
      v-model="model"
      :enable-return-focus="true"
      :show-cancel-button="false"
      :show-ok-button="results.length > 0"
      @on-ok="closeMe"
      ok-button-label="Done"
      ok-button-icon="fas fa-check"
      data-cy="addSkillEventBatchDialog">

    <Stepper v-if="results.length === 0" :linear="true" value="1" class="mb-4">
      <StepList>
        <Step value="1">Select Date</Step>
        <Step value="2">Add Users</Step>
        <Step value="3">Confirm</Step>
      </StepList>
      <StepPanels>
        <StepPanel value="1" v-slot="{ activateCallback }">
          <Message class="mb-2" data-cy="skillsToAdd" :closable="false" severity="info">
            This will add skill events for the skills <span class="font-bold">{{ skills.map( skill => skill.name ).join(', ')}}</span>
          </Message>

          <div class="mb-2 mt-4 flex flex-1 items-center">
            Select the date the events occurred:
            <SkillsCalendarInput class="mx-1 md:mx-2 md:my-0"
                                 selectionMode="single"
                                 name="eventDatePicker"
                                 v-model="dateAdded"
                                 data-cy="eventDatePicker"
                                 :max-date="new Date()"
                                 aria-label="event date" ref="eventDatePicker"/>
          </div>
          <div class="flex pt-6 justify-between">
            <SkillsButton label="Cancel" icon="far fa-times-circle" outlined class="mr-2" severity="secondary" data-cy="closeButton" @click="closeMe" />
            <SkillsButton label="Next" icon="fas fa-arrow-circle-right float-right" @click="activateCallback('2')" data-cy="firstNextButton"/>
          </div>
        </StepPanel>
        <StepPanel value="2" v-slot="{ activateCallback }">
          <Message class="mb-2" data-cy="skillsToAdd" :closable="false" severity="info">
            Select the users that have performed the skill(s).
          </Message>
          <div class="mb-2">
            <add-skill-event-form :project-id="projectId" @userAdded="addUser" :usersToAdd="usersToAdd" />

            <div class="mt-2 mb-2 flex gap-2" v-if="usersToAdd.length > 0">
              <Chip v-for="(user) of usersToAdd" :key="user.userId" :label="user.userId" data-cy="usersToAdd" removable @remove="usersToAdd.splice(usersToAdd.indexOf(user), 1)" />
            </div>
          </div>

          <div class="flex pt-6 justify-between">
            <SkillsButton label="Back" icon="fas fa-arrow-circle-left" outlined class="mr-2" severity="secondary" @click="activateCallback('1')" data-cy="backButton" />
            <SkillsButton label="Next" icon="fas fa-arrow-circle-right float-right" @click="activateCallback('3')" :disabled="usersToAdd.length === 0" data-cy="firstNextButton"/>
          </div>
        </StepPanel>
        <StepPanel value="3" v-slot="{ activateCallback }">
          <Message>
            Skill events for <span class="font-bold">{{ skills.map( skill => skill.name ).join(', ')}}</span> will be added for the
            user(s) <span class="font-bold">{{ usersToAdd.map( user => user.userIdForDisplay ? user.userIdForDisplay : user.userId ).join(', ') }}</span> on {{ dayjs(dateAdded).format('YYYY-MM-DD') }}.
            Please click "Add Events" to confirm.
          </Message>
          <div class="flex pt-6 justify-between">
            <SkillsButton label="Back" icon="fas fa-arrow-circle-left" outlined class="mr-2" severity="secondary" @click="activateCallback('2')" data-cy="backButton" />
            <SkillsButton variant="outline-primary" :disabled="usersToAdd.length === 0 || !dateAdded" data-cy="saveBatchSkillEvents" @click="saveEvents" label="Add Events" />
          </div>
        </StepPanel>
      </StepPanels>
    </Stepper>

    <SkillsDataTable
        aria-label="Admin Group Global Badges Table"
        class="mb-4"
        :value="results"
        paginator
        :rows="10"
        :totalRecords="results.length"
        v-if="results.length > 0"
        data-cy="skillEventBatchResult" table-stored-state-id="skillEventBatchResultTable">
      <Column header="" field="skillApplied" :sortable="false" style="width: 1rem">
        <template #body="slotProps">
            <i :class="[slotProps.data.success && slotProps.data.skillApplied ? 'fa fa-check text-primary' : 'fa fa-info-circle text-red-800']" aria-hidden="true"/>
        </template>
      </Column>
      <Column header="User" field="userId" :sortable="true"></Column>
      <Column header="Skill" field="skillId" :sortable="true"></Column>
      <Column header="Explanation" field="explanation" :sortable="true"></Column>
    </SkillsDataTable>
  </SkillsDialog>
</template>

<style scoped>

</style>