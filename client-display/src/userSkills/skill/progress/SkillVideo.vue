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
  <div v-if="skill.videoSummary && skill.videoSummary.videoUrl">
    <div v-if="videoCollapsed && !skill.isLocked" class="alert alert-info">
      <div class="row">
        <div class="col my-auto"><i class="fas fa-tv mr-1" style="font-size: 1.2rem;" aria-hidden="true"/> This {{ this.skillDisplayName }} has a video.</div>
        <div class="col-auto text-right"><b-button variant="info" @click="videoCollapsed = false"><i class="fas fa-play"></i> Watch</b-button></div>
      </div>
    </div>
    <b-overlay v-if="skill.isLocked" :show="true" :no-fade="true">
      <template #overlay>
        <div class="text-center text-primary" style="color: #143740 !important;">
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
    <video-player v-if="!videoCollapsed && !skill.isLocked" :options="{
              url: skill.videoSummary.videoUrl,
              type: null,
              captionsUrl: null,
            }" @watched-progress="updateVideoProgress" />
    <div v-if="!isAlreadyAchieved && !justAchieved && !skill.isLocked && !videoCollapsed" class="alert alert-info mt-2">
      <div class="row">
        <div class="col">
          <i class="fas fa-video font-size-2 mr-1" aria-hidden="true"></i>
          Earn <b>{{ skill.totalPoints }}</b> for the  {{ skillDisplayName.toLowerCase() }} by watching this Video.
        </div>
        <div class="col-auto text-right">
          <span class="font-italic">Watched: </span> <b>{{ percentWatched }}</b>%
        </div>
      </div>
    </div>
    <div v-if="justAchieved" class="alert alert-success mt-2">
      <i class="fas fa-birthday-cake text-success mr-1" style="font-size: 1.2rem"></i> Congrats! You just earned <span
        class="text-success font-weight-bold">{{ skill.totalPoints }}</span> points<span> and <b>completed</b> the {{ skillDisplayName.toLowerCase() }}</span>!
    </div>

    <div v-if="errNotification.enable" class="alert alert-danger mt-2" role="alert" data-cy="selfReportError">
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
    computed: {
      isAlreadyAchieved() {
        return this.skill.points > 0;
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
        justAchieved: false,
      };
    },
    methods: {
      updateVideoProgress(watchProgress) {
        this.percentWatched = watchProgress.percentWatched;
        if (watchProgress.percentWatched > 96) {
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
    },
  };
</script>

<style scoped>
</style>
