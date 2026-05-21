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
import {computed, onMounted, ref} from "vue";
import SkillsCalendarInput from "@/components/utils/inputForm/SkillsCalendarInput.vue";
import SkillsService from "@/components/skills/SkillsService.js";
import StepList from "primevue/steplist";
import Step from "primevue/step";
import StepPanel from "primevue/steppanel";
import StepPanels from "primevue/steppanels";
import Stepper from 'primevue/stepper'
import Column from "primevue/column";
import dayjs from "dayjs";
import SkillsSpinner from "@/components/utils/SkillsSpinner.vue";
import {useAppConfig} from "@/common-components/stores/UseAppConfig.js";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";

const model = defineModel()
const appConfig = useAppConfig();
const pluralize = usePluralize();
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

const loading = ref(false);
const usersToAdd = ref("");
const dateAdded = ref(new Date());
const results = ref([]);
const displayFullText = ref(false);
const userSuggestOptions = ref([]);
const selectedSuggestOption = ref(null);

onMounted(() => {
  if (appConfig.userSuggestOptions) {
    userSuggestOptions.value = appConfig.userSuggestOptions.split(',')
    selectedSuggestOption.value = userSuggestOptions.value[0];
  }
});

const hasUserSuggestOptions = computed(() => {
  return userSuggestOptions.value && userSuggestOptions.value.length > 0;
});

const userList = computed(() => {
  if (!usersToAdd.value) return [];

  const cleanedUsers = usersToAdd.value
      .split('\n')
      .map(user => user.trim().toLowerCase())
      .filter(user => user !== '');

  return [...new Set(cleanedUsers)];
});

const tooManyEvents = computed(() => {
  return (userList.value.length * props.skills.length) > maxSkillBatchSize.value;
})

const saveEvents = () => {
  const userIds = userList.value;
  const skillIds = props.skills.map((skill) => skill.skillId);
  loading.value = true;

  return SkillsService.saveSkillEventBatch(props.projectId, skillIds, userIds, dateAdded.value.getTime(), true, selectedSuggestOption.value).then((result) => {
    usersToAdd.value = '';
    results.value = result.results;
    loading.value = false;
  })

}

const closeMe = () => {
  model.value = false;
}

const maxSkillBatchSize = computed(() => {
  return appConfig.maxSkillBatchSize ? appConfig.maxSkillBatchSize : 200
})
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

    <Stepper v-if="results.length === 0 && !loading" :linear="true" value="1" class="mb-4">
      <StepList>
        <Step value="1">Select Date</Step>
        <Step value="2">Add Users</Step>
        <Step value="3">Confirm</Step>
      </StepList>
      <StepPanels>
        <StepPanel value="1" v-slot="{ activateCallback }">
          <Message class="mb-2" data-cy="skillsToAdd" :closable="false" severity="info">
            This will add skill events for {{ skills.length }} skill(s):
            <ul v-if="skills.length > 5 && !displayFullText" class="ml-4">
                <li class="font-bold" v-for="skill of skills.slice(0, 5)">- {{ skill.name }}</li>
            </ul>
            <ul v-else class="ml-4">
              <li class="font-bold" v-for="skill of skills">- {{ skill.name }}</li>
            </ul>
            <a v-if="skills.length > 5" size="xs" variant="outline-info"
               class="cursor-pointer"
               @click="displayFullText = !displayFullText"
               aria-label="Show/Hide truncated text"
               data-cy="showMoreOrLessBtn">
              <div v-if="displayFullText" data-cy="showLess"> Show less</div>
              <div v-else data-cy="showMore"><em>Show more</em></div>
            </a>
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
          <div v-if="hasUserSuggestOptions" class="flex gap-1 items-center mb-3">
            <Select  data-cy="userSuggestOptionsDropdown"
                     v-model="selectedSuggestOption"
                     :options="userSuggestOptions"
                     class="flex-1"/>
          </div>
          <div class="mb-2">
            <SkillsTextarea
                label="Users to add skill events for (one user per line)"
                :is-required="true"
                :submit-on-enter="false"
                v-model="usersToAdd"
                data-cy="batchUserList"
                rows="10"
                name="usersToAdd">
            </SkillsTextarea>
          </div>

          <div class="flex pt-6 justify-between">
            <SkillsButton label="Back" icon="fas fa-arrow-circle-left" outlined class="mr-2" severity="secondary" @click="activateCallback('1')" data-cy="secondBackButton" />
            <SkillsButton label="Next" icon="fas fa-arrow-circle-right float-right" @click="activateCallback('3')" :disabled="usersToAdd.length === 0" data-cy="secondNextButton"/>
          </div>
        </StepPanel>
        <StepPanel value="3" v-slot="{ activateCallback }">
          <Message :closable="false" v-if="!tooManyEvents" data-cy="confirmMessage">
            Skill events for <span class="font-bold">{{ skills.length }}</span> {{ pluralize.plural('skill', skills.length)}} will be added for <span class="font-bold">{{ userList.length }}</span> {{ pluralize.plural('user', userList.length)}} on {{ dayjs(dateAdded).format('YYYY-MM-DD') }}.
            Please click "Add Events" to confirm.
          </Message>
          <Message :closable="false" v-if="tooManyEvents" severity="error" data-cy="batchErrorMessage">
            Your batch exceeds the {{ maxSkillBatchSize }} request limit ({{skills.length}} {{ pluralize.plural('skill', skills.length) }} × {{ userList.length }} {{ pluralize.plural('users', userList.length) }}). To proceed, please remove either users or skills to reduce the total number of requests.
          </Message>
          <div class="flex pt-6 justify-between">
            <SkillsButton label="Back" icon="fas fa-arrow-circle-left" outlined class="mr-2" severity="secondary" @click="activateCallback('2')" data-cy="lastBackButton" />
            <SkillsButton variant="outline-primary" :disabled="usersToAdd.length === 0 || !dateAdded || tooManyEvents" data-cy="saveBatchSkillEvents" @click="saveEvents" label="Add Events" />
          </div>
        </StepPanel>
      </StepPanels>
    </Stepper>

    <SkillsSpinner :is-loading="loading" class="my-8" />

    <SkillsDataTable
        aria-label="Admin Group Global Badges Table"
        class="mb-4"
        :value="results"
        paginator
        :rows="10"
        :totalRecords="results.length"
        v-if="results.length > 0"
        data-cy="skillEventBatchResult" table-stored-state-id="skillEventBatchResultTable">
      <Column header="Result" field="skillApplied" :sortable="true" style="width: 8rem">
        <template #body="slotProps">
            <i class="mr-2" :class="[slotProps.data.success && slotProps.data.skillApplied ? 'fa fa-check text-primary' : 'fa fa-info-circle text-red-800']" aria-hidden="true"/> {{ slotProps.data.success && slotProps.data.skillApplied ? 'Applied' : 'Rejected' }}
        </template>
      </Column>
      <Column header="User" field="userIdForDisplay" :sortable="true"></Column>
      <Column header="Skill" field="skillId" :sortable="true"></Column>
      <Column header="Explanation" field="explanation" :sortable="true"></Column>
    </SkillsDataTable>
  </SkillsDialog>
</template>

<style scoped>

</style>