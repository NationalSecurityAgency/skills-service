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
import { useRouter } from 'vue-router';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js';
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsDisplayInfo } from '@/skills-display/UseSkillsDisplayInfo.js';
import { useSkillsDisplayAttributesState } from '@/skills-display/stores/UseSkillsDisplayAttributesState.js';
import SkillsButton from '@/components/utils/inputForm/SkillsButton.vue';
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue';
import SkillsOverlay from '@/components/utils/SkillsOverlay.vue';

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

const emit = defineEmits(['points-earned'])
const router = useRouter()
const announcer = useSkillsAnnouncer()
const skillsDisplayInfo = useSkillsDisplayInfo()
const attributes = useSkillsDisplayAttributesState()

const skillsDisplayService = useSkillsDisplayService()

const videoCollapsed = ref(props.videoCollapsedByDefault);
const percentWatched = ref(0);
const errNotification = ref({
  enable: false,
  msg: '',
});
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

const isAlreadyAchieved = computed(() => {
  return props.skill.points > 0;
});
const videoConf = computed(() => {
  const captionsUrl = props.skill.videoSummary.hasCaptions
      ? `/api/projects/${props.skill.projectId}/skills/${props.skill.skillId}/videoCaptions`
      : null;
  return {
    url: props.skill.videoSummary.videoUrl,
    videoType: props.skill.videoSummary.videoType ? props.skill.videoSummary.videoType : null,
    captionsUrl,
  };
});
const isSelfReportTypeVideo = computed(() => {
  return props.skill.selfReporting.enabled && props.skill.selfReporting.type === 'Video';
});
const transcriptWithNewLines = computed(() => {
  if (transcript.value.transcript) {
    return transcript.value.transcript.replace(/(?:\r\n|\r|\n)/g, '<br>');
  }
  return transcript.value.transcript;
});

onMounted(() => {
  trackAchievement.value = props.skill.selfReporting.enabled && props.skill.selfReporting.type && props.skill.selfReporting.type === 'Video';
})

const updateVideoProgress = (watchProgress) => {
  if (isFirstTime.value && watchProgress.videoDuration === Infinity) {
    showPercent.value = false;
  }
  isFirstTime.value = false;
  percentWatched.value = watchProgress.percentWatched;
  if (trackAchievement.value && watchProgress.percentWatched > 96 && !justAchieved.value) {
    doReportSkill();
  }
};
const achieveSkillByReadingTranscript = () => {
  doReportSkill()
      .then(() => {
        nextTick(() => {
          const element = document.getElementById('watchVideoAlert');
          if (element) {
            element.scrollIntoView({ behavior: 'smooth' });
          }
        });
      });
};
const doReportSkill = () => {
  return skillsDisplayService.reportSkill(props.skill.skillId)
      .then((res) => {
        if (res.pointsEarned > 0) {
          justAchieved.value = true;
          emit('points-earned', res.pointsEarned);
          nextTick(() => announcer.polite(`Congratulations! You just earned ${res.pointsEarned} points and completed ${props.skill.skill} skill`));
        }
      }).catch((e) => {
        if (e.response.data && e.response.data.errorCode
            && (e.response.data.errorCode === 'InsufficientProjectPoints' || e.response.data.errorCode === 'InsufficientSubjectPoints')) {
          errNotification.value.msg = e.response.data.explanation;
          errNotification.value.enable = true;
        } else {
          const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
          router.push({
            name: 'error',
            params: {
              errorMessage,
            },
          });
        }
      });
};
const loadTranscript = () => {
  transcript.value.loading = true;
  skillsDisplayService.getVideoTranscript(props.skill.skillId)
      .then((res) => {
        transcript.value.transcript = res;
        transcript.value.loading = false;
        transcript.value.show = true;
        nextTick(() => announcer.polite('Transcript displayed'));
      });
};

</script>

<template>
  <div v-if="skill.videoSummary && skill.videoSummary.videoUrl" :data-cy="`skillVideo-${skill.skillId}`">
    <Message v-if="videoCollapsed" severity="info" :closable="false" data-cy="videoCollapsed">
      <template #container>
        <div class="flex align-items-center p-3">
          <div class="flex-1"><i class="fas fa-tv mr-1" style="font-size: 1.2rem;" aria-hidden="true"/> This
            {{ attributes.skillDisplayName }} has a video.
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
        <div class="text-center text-primary surface-overlay p-2 border-round" data-cy="videoIsLockedMsg">
          <i class="fas fa-lock" style="font-size: 1.2rem;"></i>
          <div class="font-weight-bold">Complete this {{ attributes.skillDisplayName.toLowerCase() }}'s prerequisites to unlock the video</div>
        </div>
      </template>
      <div class="alert" style="padding: 0rem 1rem 0rem 1rem !important;">
        <div style="height: 400px; background-color: black;" class="row align-items-center">
          <div class="col text-center" style="color:#fff!important">
              <span class="border rounded d-inline-block p-3 pl-4">
                <i class="fas fa-play" style="font-size: 3rem;"></i>
              </span>
          </div>
        </div>
      </div>
    </SkillsOverlay>
    <div v-if="!videoCollapsed && !isLocked">
      <video-player :options="videoConf" @watched-progress="updateVideoProgress" />
      <Message v-if="isSelfReportTypeVideo && (!isAlreadyAchieved || justAchieved)"
           class="mt-2"
           :severity="justAchieved ? 'success' : 'info'"
           ref="watchVideoAlert"
           id="watchVideoAlert"
           :closable="false"
           data-cy="watchVideoAlert">
        <template #container>
          <div class="flex align-items-center p-3">
            <div class="flex-1" data-cy="watchVideoMsg">
              <div v-if="!justAchieved">
                <i class="fas fa-video font-size-2 mr-1 animate__bounceIn" aria-hidden="true"></i>
                Earn <b>{{ skill.totalPoints }}</b> points for the  {{ attributes.skillDisplayName.toLowerCase() }} by watching this Video.
              </div>
              <div v-if="justAchieved">
                <i class="fas fa-birthday-cake text-success mr-1 animate__bounceIn" style="font-size: 1.2rem"></i> Congrats! You just earned <span
                  class="text-success font-weight-bold">{{ skill.totalPoints }}</span> points<span> and <b>completed</b> the {{ attributes.skillDisplayName.toLowerCase() }}</span>!
              </div>
            </div>
            <div class="flex align-items-center">
              <span v-if="skill.videoSummary.hasTranscript">
                <SkillsSpinner :is-loading="transcript.loading" small/>
                <SkillsButton style="text-decoration: underline; padding-right: 0.25rem; padding-left: 0.5rem;"
                              class="skills-theme-primary-color"
                              label="View Transcript"
                              variant="link"
                              size="small"
                              text
                              data-cy="viewTranscriptBtn"
                              @click="loadTranscript" />
              </span>
              <span aria-hidden="true" class="mr-1" v-if="showPercent && skill.videoSummary.hasTranscript">|</span>
              <span v-if="showPercent"><span class="font-italic">Watched: </span> <b data-cy="percentWatched">{{ percentWatched }}</b>%</span>
            </div>
          </div>
        </template>
      </Message>
      <div v-if="skill.videoSummary.hasTranscript && (!isSelfReportTypeVideo || (isAlreadyAchieved && !justAchieved))" class="text-right">
        <SkillsSpinner :is-loading="transcript.loading" small />
        <SkillsButton style="text-decoration: underline; padding-right: 0.25rem; padding-left: 0.5rem;"
                      class="skills-theme-primary-color"
                      label="View Transcript 2"
                      variant="link"
                      size="small"
                      text
                      data-cy="viewTranscriptBtn"
                      @click="loadTranscript">

        </SkillsButton>
      </div>
      <Card v-if="transcript.show" class="mt-1 skills-card-theme-border">
        <template #content>
          <label for="transcriptDisplay" class="h4">Video Transcript:</label>
          <Panel id="transcriptDisplay" data-cy="videoTranscript">
            <p class="m-0">{{ transcript.transcript }}</p>
          </Panel>
          <div v-if="isSelfReportTypeVideo && !isAlreadyAchieved && !justAchieved" class="mt-2 flex align-items-center">
            <div class="flex flex-1">
              <Checkbox
                  inputId="readTranscript"
                  :binary="true"
                  name="Transcript Certification"
                  v-model="transcriptReadCert"
                  data-cy="certifyTranscriptReadCheckbox"
              />
              <label for="readTranscript" class="ml-2">I <b>certify</b> that I fully read the transcript. Please award the skill and its <Tag>{{skill.totalPoints}}</Tag> points.</label>
            </div>
            <div class="flex">
              <SkillsButton
                  severity="success"
                  label="Claim Points"
                  icon="fas fa-check-double"
                  outlined :disabled="!transcriptReadCert"
                        data-cy="claimPtsByReadingTranscriptBtn"
                        @click="achieveSkillByReadingTranscript">
                
              </SkillsButton>
            </div>
          </div>
        </template>
      </Card>
    </div>

    <Message v-if="errNotification.enable" severity="error" :closable="false" class="mt-2" role="alert" data-cy="videoError">
      <i class="fas fa-exclamation-triangle" /> {{ errNotification.msg }}
    </Message>
  </div>
</template>

<style scoped>

</style>