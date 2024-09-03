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

import { computed, onMounted, ref } from 'vue';
import { useRoute } from 'vue-router'
import { useQuizSummaryState } from '@/stores/UseQuizSummaryState.js';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import SubPageHeader from '@/components/utils/pages/SubPageHeader.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import QuizService from '@/components/quiz/QuizService.js';
import NoContent2 from '@/components/utils/NoContent2.vue';
import QuizSettingsForm from "@/components/quiz/QuizSettingsForm.vue";

const announcer = useSkillsAnnouncer();
const route = useRoute();
const quizSummaryState = useQuizSummaryState()
const isLoadingSettings = ref(true);
const isSaving = ref(false);
const quizId = ref(route.params.quizId);
const showSavedMsg = ref(false);
const settings = ref({
  passingReq: {
    value: '-1',
    setting: 'quizPassingReq',
  },
  numAttempts: {
    value: 3,
    unlimited: true,
    setting: 'quizNumberOfAttempts',
  },
  randomizeQuestions: {
    value: false,
    setting: 'quizRandomizeQuestions',
  },
  randomizeAnswers: {
    value: false,
    setting: 'quizRandomizeAnswers',
  },
  multipleTakes: {
    value: false,
    setting: 'quizMultipleTakes',
  },
  quizLength: {
    value: '-1',
    setting: 'quizLength',
  },
  quizTimeLimit: {
    value: 3600,
    unlimited: true,
    setting: 'quizTimeLimit',
  },
});
const hoursForQuiz = ref(1)
const minutesForQuiz = ref( 0)
const initialValues = ref({})

const isLoadingData = computed(() => {
  return isLoadingSettings.value || quizSummaryState.loadingQuizSummary
})

const isSurveyType = computed(() => {
  return quizSummaryState.quizSummary && quizSummaryState.quizSummary.type === 'Survey';
})

onMounted(() => {
  isLoadingSettings.value =  true;
  loadAndUpdateQuizSettings()
      .then(() => {
        isLoadingSettings.value =  false;
      });
})

const loadAndUpdateQuizSettings = () => {
    return QuizService.getQuizSettings(quizId.value)
      .then((resSettings) => {
        if (resSettings) {
          const settingsKeys = Object.keys(settings.value);
          settingsKeys.forEach((key) => {
            const settingValue = settings.value[key];
            const foundFromServer = resSettings.find((confS) => settingValue.setting === confS.setting);
            if (foundFromServer) {
              if (foundFromServer.setting === settings.value.numAttempts.setting) {
                if (Number(foundFromServer.value) === -1) {
                  settings.value.numAttempts.value = 3;
                  settings.value.numAttempts.unlimited = true;
                } else {
                  settings.value.numAttempts.value = Number(foundFromServer.value);
                  settings.value.numAttempts.unlimited = false;
                }
              } else if (foundFromServer.setting === settings.value.quizTimeLimit.setting) {
                if (Number(foundFromServer.value) === -1) {
                  settings.value.quizTimeLimit.value = 3600;
                  hoursForQuiz.value =  1;
                  minutesForQuiz.value =  0;
                  settings.value.quizTimeLimit.unlimited = true;
                } else {
                  settings.value.quizTimeLimit.value = Number(foundFromServer.value);
                  hoursForQuiz.value = Math.floor(foundFromServer.value / 3600);
                  const remainingTime = foundFromServer.value - (hoursForQuiz.value * 3600);
                  minutesForQuiz.value =  remainingTime / 60;
                  settings.value.quizTimeLimit.unlimited = false;
                }
              } else if ([settings.value.randomizeQuestions.setting, settings.value.randomizeAnswers.setting, settings.value.multipleTakes.setting].includes(foundFromServer.setting)) {
                settings.value[key].value = (/true/i).test(foundFromServer.value);
              } else {
                settings.value[key].value = foundFromServer.value;
              }
            }
          });

          initialValues.value = {
              quizLength: settings.value.quizLength.value,
              quizPassingReq: settings.value.passingReq.value,
              quizNumberOfAttemptsUnlimited: settings.value.numAttempts.unlimited,
              quizNumberOfAttempts: settings.value.numAttempts.value,
              quizRandomizeQuestions: settings.value.randomizeQuestions.value,
              quizRandomizeAnswers: settings.value.randomizeAnswers.value,
              quizMultipleTakes: settings.value.multipleTakes.value,
              quizTimeLimitUnlimited: settings.value.quizTimeLimit.unlimited,
              quizTimeLimitHours: hoursForQuiz.value,
              quizTimeLimitMinutes: minutesForQuiz.value
          }
        }
      });
}

const collectAndSave = (values) => {
  let dirtySettings = values;

  if (dirtySettings) {
    isSaving.value = true;
    dirtySettings = dirtySettings.map((s) => {
      if ((s.setting === settings.value.numAttempts.setting || s.setting === settings.value.quizTimeLimit.setting) && s.unlimited) {
        return ({ ...s, value: '-1' });
      }
      return s;
    });

    QuizService.saveQuizSettings(quizId.value, dirtySettings).then(() => {
      loadAndUpdateQuizSettings().then(() => {
        isSaving.value = false;
        showSavedMsg.value = true;
        announcer.polite('Quiz Settings have been successfully saved');
        setTimeout(() => {
          showSavedMsg.value = false;
        }, 4000);
      });
    });
  }
};
</script>

<template>
  <div>
    <SubPageHeader title="Settings" aria-label="Settings" />
    <SkillsSpinner :is-loading="isLoadingData"/>
    <Card>
      <template #content>
        <div v-if="!isLoadingData">
          <QuizSettingsForm :isSurvey="isSurveyType" :initialValues="initialValues" @saveSettings="collectAndSave" :quizSummaryState="quizSummaryState" :showSavedMsg="showSavedMsg" />
        </div>
      </template>
    </Card>
  </div>
</template>

<style scoped>

</style>