/*
Copyright 2024 SkillTree

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
import { computed, onMounted, ref, nextTick } from 'vue'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js';
import { useSkillsState } from '@/stores/UseSkillsState.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useRoute, useRouter } from 'vue-router'
import SkillsService from '@/components/skills/SkillsService.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import ExistingUserInput from '@/components/utils/ExistingUserInput.vue';
import SkillsCalendarInput from '@/components/utils/inputForm/SkillsCalendarInput.vue'
import ProjectService from '@/components/projects/ProjectService.js';
import * as yup from 'yup';
import { useForm } from 'vee-validate';

const props = defineProps({
  projectId: String,
})

const appConfig = useAppConfig()
const skillsState = useSkillsState()
const route = useRoute()
const router = useRouter()
const announcer = useSkillsAnnouncer()

const dateAdded = ref(new Date());
const usersAdded = ref([]);
const isSaving = ref(false);
const isLoading = ref(true);
const currentSelectedUser = ref(null);
const projectTotalPoints = ref(0);

const reversedUsersAdded = computed(() => {
  return usersAdded.value.map((e) => e)
      .reverse();
});
const minimumPoints = computed(() => {
  return appConfig.minimumProjectPoints;
});
const disable = computed(() => {
  return (!currentSelectedUser.value || !currentSelectedUser.value || !currentSelectedUser.value.userId || currentSelectedUser.value.userId.length === 0)
      || projectTotalPoints.value < minimumPoints.value
      || (skillsState?.groupId && !skillsState?.enabled);
});
const minPointsTooltip = computed(() => {
  let text = '';

  if (skillsState?.groupId && !skillsState?.enabled) {
    text = 'Unable to add skill for user. This skill belongs to a group whose current status is disabled';
  } else if (projectTotalPoints.value < minimumPoints.value) {
    text = 'Unable to add skill for user. Insufficient available points in project.';
  }
  return text;
});
const addButtonIcon = computed(() => {
  if (projectTotalPoints.value >= minimumPoints.value) {
    return isSaving.value ? 'fa fa-circle-notch fa-spin fa-3x-fa-fw' : 'fas fa-arrow-circle-right';
  }
  return 'icon-warning fa fa-exclamation-circle text-warning';
});

const newUserObjNoSpacesValidatorInNonPkiMode = (value) => {
  if (appConfig.isPkiAuthenticated || !value.userId) {
    return true;
  }
  const hasSpaces = value.userId.indexOf(' ') >= 0;
  return !hasSpaces;
}

const schema = yup.object().shape({
  'userIdInput': yup.mixed().transform((value, input, ctx) => {
        if (typeof value === 'string') {
          return {
            userId: value,
          }
        }
        return value;
      })
      .required()
      .test('newUserObjNoSpacesValidatorInNonPkiMode', 'User Id may not contain spaces', (value) => newUserObjNoSpacesValidatorInNonPkiMode(value))
      .label('User Id'),
  'eventDatePicker': yup.date()
      .required()
      .label('Event Date'),
})

const { values, meta, handleSubmit, isSubmitting, setFieldValue, validate, errors, resetForm } = useForm({
  validationSchema: schema,
  initialValues: {
    userIdInput: null,
    eventDatePicker: dateAdded.value
  }
})

onMounted(() => {
  isLoading.value = true;
  loadProject()
});

const loadProject = () => {
  ProjectService.getProject(props.projectId).then((res) => {
    projectTotalPoints.value = res.totalPoints;
    isLoading.value = false;
  });
}
const addSkill = () => {
  isSaving.value = true;
  SkillsService.saveSkillEvent(route.params.projectId, route.params.skillId, currentSelectedUser.value, dateAdded.value.getTime(), appConfig.isPkiAuthenticated)
      .then((data) => {
        isSaving.value = false;
        const historyObj = {
          success: data.skillApplied,
          msg: data.explanation,
          userId: currentSelectedUser.value.userId,
          userIdForDisplay: currentSelectedUser.value.userIdForDisplay,
          key: currentSelectedUser.value.userId + new Date().getTime() + data.skillApplied,
        };
        const { userId } = currentSelectedUser.value;
        if (!data.skillApplied) {
          nextTick(() => announcer.polite(`Could not add Skill event for ${userId}, ${data.explanation}`));
        } else {
          nextTick(() => announcer.polite(`Skill event has been added for ${userId}`));
        }
        usersAdded.value.push(historyObj);
        currentSelectedUser.value = null;
        resetForm();
      })
      .catch((e) => {
        const hasErrorCode = e.response.data && e.response.data.errorCode;
        if (hasErrorCode && (e.response.data.errorCode === 'UserNotFound' || e.response.data.errorCode === 'SkillEventForQuizSkillIsNotAllowed')) {
          isSaving.value = false;
          const historyObj = {
            success: false,
            msg: e.response.data.explanation,
            userId: currentSelectedUser.value.userId,
            userIdForDisplay: currentSelectedUser.value.userIdForDisplay,
            key: currentSelectedUser.value.userId + new Date().getTime() + false,
          };
          usersAdded.value.push(historyObj);
          currentSelectedUser.value = null;
          resetForm();
        } else {
          const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
          router.push({ name: 'ErrorPage', query: { errorMessage } });
        }
      })
      .finally(() => {
        isSaving.value = false;
      });
}
</script>

<template>
  <div>
    <SubPageHeader title="Add Skill Events" />
    <Card>
      <template #content>
        <div class="flex flex-wrap align-items-start">
          <div class="flex flex-1 px-1">
            <existing-user-input class="w-full"
                                 :project-id="projectId"
                                 v-model="currentSelectedUser"
                                 :can-enter-new-user="!appConfig.isPkiAuthenticated"
                                 name="userIdInput"
                                 aria-errormessage="userIdInputError"
                                 aria-describedby="userIdInputError"
                                 :aria-invalid="!meta.valid"
                                 data-cy="userIdInput" />
          </div>
          <div class="flex">
            <SkillsCalendarInput class="mx-2 my-0" selectionMode="single" name="eventDatePicker" v-model="dateAdded" data-cy="eventDatePicker"
                                 :max-date="new Date()" aria-label="event date" ref="eventDatePicker" />
          </div>
          <div class="flex">
            <SkillsButton
                aria-label="Add Specific User"
                data-cy="addSkillEventButton"
                v-skills="'ManuallyAddSkillEvent'"
                @click="addSkill"
                :disabled="!meta.valid || disable"
                :icon="addButtonIcon" label="Add">
            </SkillsButton>
          </div>
        </div>
        <Message v-if="!isLoading && minPointsTooltip" severity="warn" :closable="false">{{ minPointsTooltip }}</Message>
        <div class="mt-5" v-for="(user) in reversedUsersAdded" v-bind:key="user.key" data-cy="addedUserEventsInfo">
          <div class="">
            <span :class="[user.success ? 'text-primary' : 'text-red-800']" style="font-weight: bolder">
              <i :class="[user.success ? 'fa fa-check' : 'fa fa-info-circle']" aria-hidden="true"/>
              <span v-if="user.success">
                Added points for
              </span>
              <span v-else>
                Unable to add points for
              </span>
              <span>[{{user.userIdForDisplay ? user.userIdForDisplay : user.userId }}]</span>
            </span><span v-if="!user.success"> - {{user.msg}}</span>
          </div>
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped></style>
