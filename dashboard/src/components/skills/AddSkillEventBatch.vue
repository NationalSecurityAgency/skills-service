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
import LengthyOperationProgressBar from "@/components/utils/LengthyOperationProgressBar.vue";
import {object, string} from "yup";
import {useForm} from "vee-validate";
import {useDebounceFn} from "@vueuse/core";
import ExistingUserInput from "@/components/utils/ExistingUserInput.vue";
import { SkillsReporter } from '@skilltree/skills-client-js'

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

const savingEvents = ref(false);
const usersToAdd = ref("");
const dateAdded = ref(new Date());
const results = ref([]);
const displayFullText = ref(false);
const userSuggestOptions = ref([]);
const selectedSuggestOption = ref(null);
const currentSelectedUser = ref(null);

onMounted(() => {
  if (appConfig.userSuggestOptions) {
    userSuggestOptions.value = appConfig.userSuggestOptions.split(',')
    selectedSuggestOption.value = userSuggestOptions.value[0];
  }
});

const parseUsersFromStr = (str) => {
  if (!str) return [];

  const cleanedUsers = str
      .split('\n')
      .map(user => user.trim().toLowerCase())
      .filter(user => user !== '');

  return [...new Set(cleanedUsers)];
}

const checkUserNameLength = useDebounceFn((value) => {
  const usersToCheck = parseUsersFromStr(value)
  return usersToCheck.find(user => user.length < 3) === undefined;
}, 500)

const checkTooManyEvents = (value) => {
  const numUsers = parseUsersFromStr(value).length;
  const totalEvents = numUsers * props.skills.length;
  return totalEvents < maxSkillBatchSize.value
}
const userList = computed(() => parseUsersFromStr(usersToAdd.value))

const numTotalEvents = computed(() => userList.value.length * props.skills.length)

const schema = object({
  'usersToAdd': string()
      .trim()
      .test('minUserLength', 'Each user must be at least 3 characters', (value) => checkUserNameLength(value))
      .test('tooManyEvents', () =>
          `Limit Exceeded: Your batch of ${numTotalEvents.value} requests (${props.skills.length} ${ pluralize.plural('skill', props.skills.length)} × ${ userList.value.length } ${pluralize.plural('users', userList.value.length)}) exceeds the ${maxSkillBatchSize.value}-request limit. Please remove users or unselect skills to proceed.`,
          (value) => checkTooManyEvents(value)
      )
      .label('Users')
})
const { meta, validate } = useForm({
  validationSchema: schema,
})

const hasUserSuggestOptions = computed(() => {
  return userSuggestOptions.value && userSuggestOptions.value.length > 0;
});

const numMillisPerSkillEvent = appConfig.numMillisPerSkillEventInBatchReporting;

// Total time in milliseconds
const totalEstimatedTime = computed(() => (numTotalEvents.value * numMillisPerSkillEvent) + 1000);

const timeoutPerProgressMovement = 300;

// 1. Find the total number of progress updates/ticks that will happen
const totalExpectedTicks = computed(() => totalEstimatedTime.value / timeoutPerProgressMovement);

// 2. Divide 100% by the total ticks to get the exact percentage step size
const incrementPerProgressMovement = computed(() => {
  if (totalExpectedTicks.value <= 0) return 100;
  return 100 / totalExpectedTicks.value;
});


const saveEvents = () => {
  const userIds = userList.value;
  const skillIds = props.skills.map((skill) => skill.skillId);
  savingEvents.value = true;

  return SkillsService.saveSkillEventBatch(props.projectId, skillIds, userIds, dateAdded.value.getTime(), true, selectedSuggestOption.value).then((result) => {
    usersToAdd.value = '';
    results.value = result.results;
    savingEvents.value = false;
    SkillsReporter.reportSkill('ManuallyAddSkillEvent')
  })

}

const closeMe = () => {
  model.value = false;
}

const maxSkillBatchSize = computed(() => {
  return appConfig.maxSkillBatchSize ? appConfig.maxSkillBatchSize : 200
})

const toStep3 = (activateCallback) => {
  validate().then(({valid}) => {
    if (valid) {
      activateCallback('3')
    }
  })
}

const addSelectedUser = () => {
  usersToAdd.value += (usersToAdd.value.length > 0 ? "\n" : "") + currentSelectedUser.value.userId + "\n";
  currentSelectedUser.value = null;
}
</script>

<template>
  <SkillsDialog
      id="addSkillEventBatch"
      header="Add Skill Events"
      v-model="model"
      :submitting="savingEvents"
      :enable-return-focus="true"
      :show-cancel-button="false"
      :show-ok-button="results.length > 0"
      @on-ok="closeMe"
      ok-button-label="Done"
      ok-button-icon="fas fa-check"
      data-cy="addSkillEventBatchDialog">

    <Stepper v-if="results.length === 0 && !savingEvents" :linear="true" value="1" class="mb-4">
      <StepList>
        <Step value="1">Select Date</Step>
        <Step value="2">Add Users</Step>
        <Step value="3">Confirm</Step>
      </StepList>
      <StepPanels>
        <StepPanel value="1" v-slot="{ activateCallback }">
          <Message class="mb-2" data-cy="skillsToAdd" :closable="false" severity="info">
            This will add skill events for {{ skills.length }} {{ pluralize.plural('skill', skills.length)}}:
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
            <div class="flex flex-1 px-1 gap-2">
              <existing-user-input class="w-full"
                                   :project-id="projectId"
                                   v-model="currentSelectedUser"
                                   :can-enter-new-user="!appConfig.isPkiAuthenticated"
                                   name="userIdInput"
                                   aria-errormessage="userIdInputError"
                                   aria-describedby="userIdInputError"
                                   :aria-invalid="!meta.valid"
                                   data-cy="userIdInput" />
              <SkillsButton label="Add" @click="addSelectedUser" :disabled="!currentSelectedUser"/>
            </div>
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
            <SkillsButton label="Next" icon="fas fa-arrow-circle-right float-right" @click="toStep3(activateCallback)" :disabled="usersToAdd.length === 0 || !meta.valid" data-cy="secondNextButton"/>
          </div>
        </StepPanel>
        <StepPanel value="3" v-slot="{ activateCallback }">
          <Message :closable="false" data-cy="confirmMessage">
            Skill events for <span class="font-bold">{{ skills.length }}</span> {{ pluralize.plural('skill', skills.length)}} will be added for <span class="font-bold">{{ userList.length }}</span> {{ pluralize.plural('user', userList.length)}} on {{ dayjs(dateAdded).format('YYYY-MM-DD') }}.
            Please click "Add Events" to confirm.
          </Message>
          <div class="flex pt-6 justify-between">
            <SkillsButton label="Back" icon="fas fa-arrow-circle-left" outlined class="mr-2" severity="secondary" @click="activateCallback('2')" data-cy="lastBackButton" />
            <SkillsButton variant="outline-primary" :disabled="usersToAdd.length === 0 || !dateAdded" data-cy="saveBatchSkillEvents" @click="saveEvents" label="Add Events" />
          </div>
        </StepPanel>
      </StepPanels>
    </Stepper>

    <div v-if="savingEvents"  class="mt-8 mb-14 flex flex-col gap-3 items-center">
      <SkillsSpinner v-if="numTotalEvents === 1" :is-loading="savingEvents" data-cy="batchSaveLoader"/>
      <div data-cy="batchSaveLoadingMsg">Reporting <Tag>{{ numTotalEvents }}</Tag> skill {{ pluralize.plural('event', numTotalEvents)}}<span v-if="numTotalEvents > 1"> ({{skills.length}} {{ pluralize.plural('skill', skills.length) }} × {{ userList.length }} {{ pluralize.plural('users', userList.length) }})</span>.</div>
      <div v-if="numTotalEvents > 1" class="w-full">
        <lengthy-operation-progress-bar
          style="height: 1.2rem"
          :show-value="false"
          :timeout="timeoutPerProgressMovement"
          :increment="incrementPerProgressMovement"
          data-cy="batchSaveProgressBar"
          aria-label="Report Skills Progress"/>
      </div>
    </div>

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