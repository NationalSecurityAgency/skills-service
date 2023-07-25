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
  <div v-if="skill.videoSummary && skill.videoSummary.videoUrl" :data-cy="`skillVideo-${skill.skillId}`">
    <div v-if="videoCollapsed" class="alert alert-info" data-cy="videoCollapsed">
      <div class="row">
        <div class="col my-auto"><i class="fas fa-tv mr-1" style="font-size: 1.2rem;" aria-hidden="true"/> This {{ this.skillDisplayName }} has a video.</div>
        <div class="col-auto text-right"><b-button variant="info" @click="videoCollapsed = false" data-cy="expandVideoBtn"><i class="fas fa-play"></i> Watch</b-button></div>
      </div>
    </div>
    <b-overlay v-if="!videoCollapsed && skill.isLocked" :show="true" :no-fade="true">
      <template #overlay>
        <div class="text-center text-primary" style="color: #143740 !important;" data-cy="videoIsLockedMsg">
          <i class="fas fa-lock" style="font-size: 1.2rem;"></i>
          <div class="font-weight-bold">Complete this {{ skillDisplayName.toLowerCase() }}'s prerequisites to unlock the video</div>
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
    </b-overlay>
    <div v-if="!videoCollapsed && !skill.isLocked">
      <video-player :options="videoConf" @watched-progress="updateVideoProgress" />
      <div v-if="isSelfReportTypeVideo && (!isAlreadyAchieved || justAchieved)"
           class="alert mt-2"
           :class="{'alert-success' : justAchieved, 'alert-info': !justAchieved}"
           data-cy="watchVideoAlert">
        <div class="row">
          <div class="col-md my-auto" data-cy="watchVideoMsg">
            <div v-if="!justAchieved">
              <i class="fas fa-video font-size-2 mr-1 animate__bounceIn" aria-hidden="true"></i>
              Earn <b>{{ skill.totalPoints }}</b> for the  {{ skillDisplayName.toLowerCase() }} by watching this Video.
            </div>
            <div v-if="justAchieved">
              <i class="fas fa-birthday-cake text-success mr-1 animate__bounceIn" style="font-size: 1.2rem"></i> Congrats! You just earned <span
                class="text-success font-weight-bold">{{ skill.totalPoints }}</span> points<span> and <b>completed</b> the {{ skillDisplayName.toLowerCase() }}</span>!
            </div>
          </div>
          <div class="col-md-auto text-right my-auto">
            <span v-if="skill.videoSummary.hasTranscript">
              <b-spinner v-if="transcript.loading" small />
              <b-button style="text-decoration: underline; padding-right: 0.25rem; padding-left: 0.5rem;"
                        variant="link"
                        class="skills-theme-primary-color"
                        data-cy="viewTranscriptBtn"
                        @click="loadTranscript">View Transcript</b-button>
              <span aria-hidden="true" class="mr-1">|</span>
            </span>
            <span class="font-italic">Watched: </span> <b data-cy="percentWatched">{{ percentWatched }}</b>%
          </div>
        </div>
      </div>
      <div v-if="skill.videoSummary.hasTranscript && (!isSelfReportTypeVideo || (isAlreadyAchieved && !justAchieved))" class="text-right">
        <b-spinner v-if="transcript.loading" small />
        <b-button style="text-decoration: underline; padding-right: 0.25rem; padding-left: 0.5rem;"
                  class="skills-theme-primary-color"
                  variant="link"
                  data-cy="viewTranscriptBtn"
                  @click="loadTranscript">View Transcript</b-button>
      </div>
      <b-card v-if="transcript.show" class="mt-1 skills-card-theme-border" data-cy="videoTranscript">
        <label for="transcriptDisplay" class="h4">Video Transcript:</label>
        <b-textarea id="transcriptDisplay" v-model="transcript.transcript" :readonly="true" rows="5"></b-textarea>
      </b-card>
    </div>

    <div v-if="errNotification.enable" class="alert alert-danger mt-2" role="alert" data-cy="videoError">
      <i class="fas fa-exclamation-triangle" /> {{ errNotification.msg }}
    </div>
  </div>
</template>

<script>
  import VideoPlayer from '@/common-components/video/VideoPlayer';
  import UserSkillsService from '@/userSkills/service/UserSkillsService';

  export default {
    name: 'SkillVideo',
    components: {
      VideoPlayer,
    },
    props: {
      skill: Object,
      videoCollapsedByDefault: {
        type: Boolean,
        default: false,
        required: false,
      },
    },
    mounted() {
      this.trackAchievement = this.skill.selfReporting.enabled && this.skill.selfReporting.type && this.skill.selfReporting.type === 'Video';
    },
    computed: {
      isAlreadyAchieved() {
        return this.skill.points > 0;
      },
      videoConf() {
        const captionsUrl = this.skill.videoSummary.hasCaptions
          ? `/api/projects/${this.skill.projectId}/skills/${this.skill.skillId}/videoCaptions`
          : null;
        return {
          url: this.skill.videoSummary.videoUrl,
          videoType: this.skill.videoSummary.videoType ? this.skill.videoSummary.videoType : null,
          captionsUrl,
        };
      },
      isSelfReportTypeVideo() {
        return this.skill.selfReporting.enabled && this.skill.selfReporting.type === 'Video';
      },
      transcriptWithNewLines() {
        if (this.transcript.transcript) {
          return this.transcript.transcript.replace(/(?:\r\n|\r|\n)/g, '<br>');
        }
        return this.transcript.transcript;
      },
    },
    data() {
      return {
        videoCollapsed: this.videoCollapsedByDefault,
        percentWatched: 0,
        errNotification: {
          enable: false,
          msg: '',
        },
        trackAchievement: true,
        justAchieved: false,
        transcript: {
          show: false,
          loading: false,
          transcript: '',
        },
      };
    },
    methods: {
      updateVideoProgress(watchProgress) {
        this.percentWatched = watchProgress.percentWatched;
        if (this.trackAchievement && watchProgress.percentWatched > 96 && !this.justAchieved) {
          UserSkillsService.reportSkill(this.skill.skillId)
            .then((res) => {
              if (res.pointsEarned > 0) {
                this.justAchieved = true;
                this.$emit('points-earned', res.pointsEarned);
              }
            }).catch((e) => {
              if (e.response.data && e.response.data.errorCode
                && (e.response.data.errorCode === 'InsufficientProjectPoints' || e.response.data.errorCode === 'InsufficientSubjectPoints')) {
                this.errNotification.msg = e.response.data.explanation;
                this.errNotification.enable = true;
              } else {
                const errorMessage = (e.response && e.response.data && e.response.data.explanation) ? e.response.data.explanation : undefined;
                this.$router.push({
                  name: 'error',
                  params: {
                    errorMessage,
                  },
                });
              }
            });
        }
      },
      loadTranscript() {
        this.transcript.loading = true;
        UserSkillsService.getVideoTranscript(this.skill.skillId)
          .then((res) => {
            this.transcript.transcript = res;
            this.transcript.loading = false;
            this.transcript.show = true;
            this.$nextTick(() => this.$announcer.polite('Transcript displayed'));
          });
      },
    },
  };
</script>

<style scoped>
</style>
