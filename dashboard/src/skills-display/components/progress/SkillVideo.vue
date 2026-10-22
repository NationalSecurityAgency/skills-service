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

import { ref, computed, onMounted, nextTick, defineAsyncComponent } from 'vue';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js';
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue';
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';
import {useNumberFormat} from "@/common-components/filter/UseNumberFormat.js";
import {useTimeUtils} from "@/common-components/utilities/UseTimeUtils.js";
import {usePluralize} from "@/components/utils/misc/UsePluralize.js";

const VideoPlayer = defineAsyncComponent(() =>
    import('@/common-components/video/VideoPlayer.vue')
)

const props = defineProps({
  skill: Object,
  videoCollapsedByDefault: {
    type: Boolean,
    default: false,
    required: false,
  },
  isLocked: Boolean,
})

const numFormat = useNumberFormat()
const timeUtils = useTimeUtils()

const emit = defineEmits(['points-earned'])
const announcer = useSkillsAnnouncer()
const pluralize = usePluralize()
const attributes = useSkillsDisplayAttributesState()

const skillsDisplayService = useSkillsDisplayService()

const videoCollapsed = ref(props.videoCollapsedByDefault);
const percentWatched = ref(0);
const errNotification = ref({
  enable: false,
  msg: '',
  retryable: false,
});
const pendingReport = ref(null);
const isReporting = ref(false);
const reportedPoints = ref(0);
const trackAchievement = ref(true);
const justAchieved = ref(false);
const transcript = ref({
  show: false,
  loading: false,
  transcript: '',
});
const showPercent = ref(true);
const isFirstTime = ref(true);
const transcriptReadCert = ref(false);

const isMotivationalSkill = computed(() => props.skill && props.skill.isMotivationalSkill)

const shouldShowPercentWatched = computed(() => showPercent.value && (!isAlreadyAchieved.value || justAchieved.value) && isSelfReportTypeVideo.value);

const isAlreadyAchieved = computed(() => {
  return props.skill.points > 0;
});
const isConfiguredVideoSize = computed(() => props.skill?.videoSummary?.width && props.skill?.videoSummary?.height)
const videoConf = computed(() => {
  const captionsUrl = props.skill.videoSummary.hasCaptions
      ? `/api/projects/${props.skill.projectId}/skills/${props.skill.skillId}/videoCaptions`
      : null;
  return {
    videoId: props.skill.skillId,
    url: props.skill.videoSummary.videoUrl,
    videoType: props.skill.videoSummary.videoType ? props.skill.videoSummary.videoType : null,
    isAudio: props.skill.videoSummary.videoType ? props.skill.videoSummary.videoType.includes('audio/') : null,
    captionsUrl,
    width: props.skill.videoSummary.width,
    height: props.skill.videoSummary.height,
    allowDownloads: props.skill.videoSummary.allowDownloads,
  };
});
const isSelfReportTypeVideo = computed(() => {
  return props.skill.selfReporting.enabled && props.skill.selfReporting.type === 'Video';
});

onMounted(() => {
  trackAchievement.value = props.skill.selfReporting.enabled && props.skill.selfReporting.type && props.skill.selfReporting.type === 'Video';
})

const resetVideoProgress = () => {
  percentWatched.value = 0
  justAchieved.value = false
}

const videoFinished = (watchProgress) => {
  if (trackAchievement.value && watchProgress.percentWatched > 96 && !justAchieved.value && !pendingReport.value) {
    doReportSkill();
  }
}

const updateVideoProgress = (watchProgress) => {
  if (isFirstTime.value && watchProgress.videoDuration === Infinity) {
    showPercent.value = false;
  }
  isFirstTime.value = false;
  percentWatched.value = watchProgress.percentWatched;
};
const achieveSkillByReadingTranscript = () => {
  doReportSkill('transcript')
      .then((saved) => {
        if (!saved) {
          return;
        }
        nextTick(() => {
          const element = document.getElementById('watchVideoAlert');
          if (element) {
            element.scrollIntoView({ behavior: 'smooth' });
          }
        });
      });
};
const doReportSkill = (source = 'playback') => {
  if (isReporting.value) {
    return Promise.resolve(false);
  }
  if (!pendingReport.value) {
    pendingReport.value = { source };
    errNotification.value = { enable: false, msg: '', retryable: false };
  }
  isReporting.value = true;
  nextTick(() => announcer.polite(`Saving your ${attributes.pointDisplayNameLower} credit.`));
  const crossProjId = props.skill.crossProject ? props.skill.projectId : null
  return skillsDisplayService.reportSkill(props.skill.skillId, null, crossProjId, {
    handleError: false,
    timeout: 30000,
  })
      .then((res) => {
        if (!res || typeof res.skillApplied !== 'boolean' || !Number.isFinite(res.pointsEarned)) {
          throw new Error('Credit reporting did not return a confirmation');
        }
        pendingReport.value = null;
        errNotification.value = { enable: false, msg: '', retryable: false };
        if (res.skillApplied) {
          justAchieved.value = true;
          reportedPoints.value = res.pointsEarned;
          emit('points-earned', res.pointsEarned);
          nextTick(() => announcer.polite(reportedPoints.value > 0
              ? `Congratulations! You just earned ${reportedPoints.value} ${attributes.pointDisplayNamePlural} for ${props.skill.skill} ${attributes.skillDisplayNameLower}.`
              : 'Your completion has been saved successfully.'));
          return true;
        } else {
          errNotification.value.msg = res.explanation;
          errNotification.value.enable = true;
          nextTick(() => announcer.polite(`Sorry, ${props.skill.skill} ${attributes.skillDisplayNameLower} could not be reported because ${res.explanation}`));
          return false;
        }
      }).catch((e) => {
        const status = e.response?.status;
        const explanation = e.response?.data?.explanation;
        const retryable = Number.isInteger(status) && status > 0 && (status < 200 || status >= 300);
        if (!retryable) {
          pendingReport.value = null;
        }
        if (status === 401) {
          errNotification.value.msg = 'Your session may have expired. Please sign in again, then retry saving your credit.';
        } else {
          errNotification.value.msg = explanation || (retryable
              ? 'Your credit could not be saved. Please retry.'
              : "We couldn't confirm that your credit was saved. Please refresh the page to check your progress.");
        }
        errNotification.value.enable = true;
        errNotification.value.retryable = retryable;
        nextTick(() => announcer.polite(errNotification.value.msg));
        return false;
      }).finally(() => {
        isReporting.value = false;
      });
};
const retryReportSkill = () => {
  if (pendingReport.value && errNotification.value.retryable) {
    return doReportSkill();
  }
};
const loadTranscript = () => {
  transcript.value.loading = true;
  if (!transcript.value.show) {
    skillsDisplayService.getVideoTranscript(props.skill.skillId)
        .then((res) => {
          transcript.value.transcript = res;
          transcript.value.loading = false;
          transcript.value.show = true;
          nextTick(() => announcer.polite('Transcript displayed'));
        });
  } else {
    transcript.value.show = false;
    transcript.value.loading = false;
  }

};
const pointsLbl = computed(() => pluralize.plural(attributes.pointDisplayNameLower, props.skill.pointIncrement))
</script>

<template>
  <div v-if="skill.videoSummary && skill.videoSummary.videoUrl" :data-cy="`skillVideo-${skill.skillId}`">
    <Message v-if="videoCollapsed" severity="info" :closable="false" data-cy="videoCollapsed">
      <template #container>
        <div class="flex items-center p-4">
          <div class="flex-1"><i class="fas fa-tv mr-1" style="font-size: 1.2rem;" aria-hidden="true"/> This
            {{ attributes.skillDisplayName }} has {{ videoConf.isAudio? 'an audio track' : 'a video'}}.
          </div>
          <div class="flex">
            <SkillsButton severity="info"
                          outlined
                          size="small"
                          @click="videoCollapsed = false"
                          data-cy="expandVideoBtn"
                          icon="fas fa-play" label="Watch"
            />
          </div>
        </div>
      </template>
    </Message>
    <SkillsOverlay v-if="!videoCollapsed && isLocked" :show="true" :no-fade="true">
      <template #overlay>
        <div class="text-center text-primary bg-surface-0 dark:bg-surface-900 p-2 rounded-border mb-20" data-cy="videoIsLockedMsg">
          <i class="fas fa-lock" style="font-size: 1.2rem;"></i>
          <div class="font-weight-bold">Complete this {{ attributes.skillDisplayNameLower }}'s prerequisites to unlock the {{ videoConf.isAudio ? 'audio' : 'video'}}</div>
        </div>
      </template>
      <div class="flex" style="padding: 0rem 1rem 0rem 1rem !important;">
        <div style="height: 400px; background-color: black;" class="flex column items-center justify-center w-full">
          <div class="text-center mt-12">
              <span class="border rounded-sm d-inline-block p-4 pl-6">
                <i class="fas fa-play" style="font-size: 3rem;"></i>
              </span>
          </div>
        </div>
      </div>
    </SkillsOverlay>
    <div v-if="!videoCollapsed && !isLocked">
      <div class="flex justify-center" :inert="pendingReport !== null">
        <div :class="{ 'flex-1' : !isConfiguredVideoSize }">
          <video-player :video-player-id="`skillVideoFor-${skill.projectId}-${skill.skillId}`"
                        :options="videoConf"
                        @watched-progress="updateVideoProgress"
                        @reset-video-progress="resetVideoProgress"
                        @video-ended="videoFinished"
                        :storeAndRecoverSizeFromStorage="true" />
        </div>
      </div>
      <div class="flex justify-center items-center">
              <span v-if="skill.videoSummary.hasTranscript">
                <SkillsButton data-cy="viewTranscriptBtn"
                              label="View Transcript"
                              icon="fa-regular fa-rectangle-list"
                              class="underline"
                              link
                              :loading="transcript.loading"
                              @click="loadTranscript">
                </SkillsButton>
              </span>

        <span aria-hidden="true" class="mr-2 text-gray-400" v-if="shouldShowPercentWatched && skill.videoSummary.hasTranscript">|</span>
        <span v-if="shouldShowPercentWatched"><span class="italic">{{ videoConf.isAudio ? 'Listened To' : 'Watched'}}: </span> <b data-cy="percentWatched">{{ percentWatched }}</b>%</span>
      </div>
      <Card v-if="transcript.show" class="mt-1 skills-card-theme-border">
        <template #title>
          {{ videoConf.isAudio ? 'Audio' : 'Video'}} Transcript:
        </template>
        <template #content>
          <p class="m-0" data-cy="videoTranscript">{{ transcript.transcript }}</p>
          <div v-if="isSelfReportTypeVideo && !isAlreadyAchieved && !justAchieved" class="mt-2 flex items-center">
            <div class="flex flex-1">
              <Checkbox
                  inputId="readTranscript"
                  :binary="true"
                  name="Transcript Certification"
                  v-model="transcriptReadCert"
                  data-cy="certifyTranscriptReadCheckbox"
              />
              <label for="readTranscript" class="ml-2">I <b>certify</b> that I fully read the transcript. Please award the {{ attributes.skillDisplayNameLower }} and its <Tag>{{skill.pointIncrement}}</Tag> {{ pointsLbl }}.</label>
            </div>
            <div class="flex">
              <SkillsButton
                  severity="success"
                  :label="`Claim ${attributes.pointDisplayNamePlural}`"
                  icon="fas fa-check-double"
                  outlined :disabled="!transcriptReadCert || pendingReport !== null"
                  :loading="isReporting"
                  data-cy="claimPtsByReadingTranscriptBtn"
                  @click="achieveSkillByReadingTranscript">

              </SkillsButton>
            </div>
          </div>
        </template>
      </Card>
      <Message v-if="isSelfReportTypeVideo && isMotivationalSkill && skill.expirationDate">
        <template #container>
          <div class="flex gap-2 p-4 content-center">
            <div>
              <i class="fas fa-user-shield text-2xl" aria-hidden="true"></i>
            </div>
            <div class="flex-1 italic pt-1" data-cy="videoAlert">
              This {{ attributes.skillDisplayNameLower }}'s achievement expires <span class="font-semibold">{{ timeUtils.relativeTime(skill.expirationDate) }}</span>, but your <span class="font-size-1">
            <Tag severity="info">{{ numFormat.pretty(skill.pointIncrement) }}</Tag></span> {{ pointsLbl }} can be retained by {{ videoConf.isAudio ? 'listening to the audio again.' : 'watching the video again.'}}
            </div>
          </div>
        </template>
      </Message>
      <Message v-if="isSelfReportTypeVideo && (!isAlreadyAchieved || justAchieved || skill.meta.inProgress || pendingReport)"
           class="mt-2"
           :severity="justAchieved ? 'success' : 'info'"
           ref="watchVideoAlert"
           id="watchVideoAlert"
           :closable="false"
           data-cy="watchVideoAlert">
        <template #container>
          <div class="flex flex-col md:flex-row items-center p-4">
            <div class="flex-1" data-cy="watchVideoMsg">
              <div v-if="isReporting" data-cy="videoCreditSaving">Saving your credit...</div>
              <div v-else-if="pendingReport">Your completion is ready to save. Retry saving your credit below.</div>
              <div v-else-if="!justAchieved">
                <i class="fas fa-video font-size-2 mr-1 animate__bounceIn" aria-hidden="true"></i>
                Earn <b>{{ skill.pointIncrement }}</b> {{ pointsLbl }} for the  {{ attributes.skillDisplayNameLower }} {{ videoConf.isAudio ? 'by listening to this Audio.' : 'by watching this Video.' }}
              </div>
              <div v-else-if="reportedPoints > 0">
                <i class="fas fa-birthday-cake text-success mr-1 animate__bounceIn" style="font-size: 1.2rem"></i> Congrats! You just earned <span
                  class="text-success font-weight-bold">{{ reportedPoints }}</span> {{ pointsLbl }}<span><span v-if="skill.points === skill.totalPoints"> and <b>completed</b> the {{ attributes.skillDisplayNameLower }}</span>!</span>
                <span v-if="(justAchieved && skill.points !== skill.totalPoints)"> But you can still earn more points by {{ videoConf.isAudio ? 'listening to the Audio' : 'watching the Video' }} again!</span>
              </div>
              <div v-else>Your completion has been saved successfully.</div>
            </div>
          </div>
        </template>
      </Message>

    </div>

    <Message v-if="errNotification.enable" severity="error" :closable="false" class="mt-2" role="alert" data-cy="videoError">
      <template #container>
        <div class="flex flex-col sm:flex-row gap-4 items-start sm:items-center p-4">
          <div class="flex-1">
            {{ errNotification.msg }}
            <div v-if="errNotification.retryable">
              You don't need to {{ pendingReport?.source === 'transcript' ? 'read the transcript' : (videoConf.isAudio ? 'listen to the audio' : 'watch the video') }} again.
            </div>
          </div>
          <SkillsButton v-if="errNotification.retryable"
                        label="Retry Saving Credit"
                        icon="fas fa-rotate-right"
                        severity="danger"
                        :loading="isReporting"
                        @click="retryReportSkill"
                        data-cy="retryVideoCreditBtn" />
        </div>
      </template>
    </Message>
  </div>
</template>

<style scoped>

</style>
