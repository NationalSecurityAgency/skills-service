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
    <div v-if=badge class="row skills-badge" :data-cy="`badge_${badge.badgeId}`">
        <div class="col-lg-2">
            <div class="card mb-2 skills-badge-icon">
                <div class="card-body">
                    <i :class="iconCss" style="font-size: 4em; min-width: 3rem;, max-width: 4rem;"/>
                    <i v-if="badge.gem" class="fas fa-gem position-absolute" style="bottom: 5px; right: 5px; color: purple"></i>
                    <i v-if="badge.global" class="fas fa-globe position-absolute" style="top: 5px; right: 5px; color: blue"></i>

                    <span v-if="badge.achievementPosition > 0 && badge.achievementPosition <= 3" class="position-absolute user-trophy">
                      <span :class="'fa-stack fa-2x ' + classNames[badge.achievementPosition - 1]" style="vertical-align: top; font-size:32px;">
                        <i class="fas fa-certificate"></i>
                        <i class="fas fa-ribbon fa-stack-1x" style="padding-top: 5px;"></i>
                        <span class="sr-only">You finished in </span>
                        <span style="font-size:.4em; color:#000000;" class="fa-stack-1x">{{positionNameShort[badge.achievementPosition - 1]}}</span>
                        <span class="sr-only"> place</span>
                      </span>
                    </span>
                    <div v-if="badge.gem" class="text-muted">
                        <small>Expires {{ badge.endDate | relativeTime() }}</small>
                    </div>
                    <div v-if="badge.global" class="text-muted">
                        <small><b>Global Badge</b></small>
                    </div>
                    <div v-else-if="displayProjectName" class="text-muted text-center text-truncate" data-cy="badgeProjectName">
                        <small>Proj<span class="d-md-none d-xl-inline">ect</span>: {{badge.projectName}}</small>
                    </div>

                    <div v-if="badge.achievedWithinExpiration" class="bonus-award mt-2 border-top">
                      <div class="award-icon"><i :class="badge.awardAttrs.iconClass + ' skills-color-orange'"></i></div>
                      <span class="sr-only">You got the </span>
                      <div style="font-size: .4em;">{{ badge.awardAttrs.name }}</div>
                      <span class="sr-only"> bonus</span>
                    </div>
                </div>
            </div>
        </div>

        <div class="text-sm-left text-center skills-text-description col-lg-10">
            <div class="row">
              <div class="h4 mb-1 col-md-8" data-cy="badgeTitle">
                <div v-if="badge.badgeHtml"  v-html="badge.badgeHtml" />
                <div v-else>{{ badge.badge }}</div>
              </div>
                <div class="col-md-4 text-right">
                    <small class=" float-right text-navy" :class="{ 'text-success': percent === 100 }">
                        <i v-if="percent === 100" class="fa fa-check"/> {{ percent }}% Complete
                    </small>
                </div>
            </div>

            <div class="mb-2">
                <progress-bar bar-color="lightgreen" :val="percent"></progress-bar>
            </div>

            <div class="alert alert-success" v-if="badge && !badge.global" style="font-size: 1.2em;">
              <div v-if="badge.numberOfUsersAchieved > 0">
                <i class="fas fa-trophy award-info-icon"></i>
                <span v-if="!badge.badgeAchieved">{{badge.numberOfUsersAchieved}} {{usersAchieved}} achieved this badge so far - <span class="time-style">you could be next!</span></span>
                <span v-else-if="badge.badgeAchieved && badge.numberOfUsersAchieved > 1">{{badge.numberOfUsersAchieved - 1}} other {{otherUsersAchieved}} achieved this badge so far</span>
                <span v-else>You've achieved this badge</span>
                <span v-if="achievementOrder !== ''"> - <span class="time-style">and you were the {{achievementOrder}}!</span></span>
              </div>
              <div v-else><i class="fas fa-car-side award-info-icon"></i>No one has achieved this badge yet - <span class="time-style">you could be the first!</span></div>

              <div v-if="badge.firstPerformedSkill && !badge.badgeAchieved">
                <i class="fas fa-clock award-info-icon"></i>You started working on this badge <span :title="badge.firstPerformedSkill" class="time-style">{{ badge.firstPerformedSkill | relativeTime() }}</span>.
                <span v-if="!badge.hasExpired && badge.expirationDate && currentTime">
                   Achieve it in
                  <span class="time-style">
                    {{ currentTime | duration(badge.expirationDate) }}
                  </span>
                  for the <i :class="badge.awardAttrs.iconClass"></i> <span class="time-style">{{ badge.awardAttrs.name }}</span> bonus!
                </span>
              </div>

              <div v-if="badge.badgeAchieved && badge.achievedWithinExpiration">
                <i :class="badge.awardAttrs.iconClass" class="award-info-icon"></i>You've earned the <span class="time-style">{{ badge.awardAttrs.name }}</span> bonus!
              </div>
            </div>

            <p v-if="badge && badge.description" class="">
               <markdown-text :text="badge.description"/>
            </p>

            <slot name="body-footer" v-bind:props="badge">

            </slot>
        </div>
    </div>
</template>

<script>
  import ProgressBar from 'vue-simple-progress';
  import dayjs from 'dayjs';
  import MarkdownText from '../utilities/MarkdownText';

  export default {
    name: 'BadgeDetailsOverview',
    components: {
      ProgressBar,
      MarkdownText,
    },
    props: {
      badge: {
        type: Object,
      },
      iconColor: {
        type: String,
        default: 'text-success',
      },
      displayProjectName: {
        type: Boolean,
        required: false,
        default: false,
      },
    },
    data() {
      return {
        positionNames: ['first', 'second', 'third'],
        positionNameShort: ['1st', '2nd', '3rd'],
        classNames: ['skills-color-gold', 'skills-color-silver', 'skills-color-bronze'],
        currentTime: null,
        timer: null,
      };
    },
    mounted() {
      this.initializeDeadlineTimer();
    },
    beforeDestroy() {
      if (this.badge.expirationDate > 0) {
        this.destroyDeadlineTimer();
      }
    },
    watch: {
      userHasPerformedSkill() {
        this.initializeDeadlineTimer();
      },
    },
    methods: {
      initializeDeadlineTimer() {
        if (this.badge.expirationDate > 0) {
          this.currentTime = dayjs().utc().valueOf();
          this.createDeadlineTimer();
        }
      },
      createDeadlineTimer() {
        this.timer = setInterval(() => {
          this.currentTime = dayjs().utc().valueOf();
          if (this.currentTime >= this.badge.expirationDate) {
            this.destroyDeadlineTimer();
          }
        }, 1000);
      },
      destroyDeadlineTimer() {
        clearInterval(this.timer);
        this.currentTime = null;
        this.timer = null;
      },
    },
    computed: {
      percent() {
        if (this.badge.numTotalSkills === 0) {
          return 0;
        }
        return Math.trunc((this.badge.numSkillsAchieved / this.badge.numTotalSkills) * 100);
      },
      iconCss() {
        return `${this.badge.iconClass} ${this.iconColor}`;
      },
      achievementOrder() {
        return this.badge.achievementPosition > 0 && this.badge.achievementPosition < 4 ? this.positionNames[this.badge.achievementPosition - 1] : '';
      },
      usersAchieved() {
        return this.badge.numberOfUsersAchieved === 1 ? 'person has' : 'people have';
      },
      otherUsersAchieved() {
        return (this.badge.numberOfUsersAchieved - 1) === 1 ? 'person has' : 'people have';
      },
      userHasPerformedSkill() {
        return this.badge.firstPerformedSkill;
      },
    },
  };
</script>

<style scoped>
  @keyframes pop-in {
    0% { opacity: 0; transform: scale(0.1); }
    100% { opacity: 1; transform: scale(1); }
  }
  .user-trophy {
    top: -8px;
    right: -20px;
    animation: pop-in 1s;
  }
  .bonus-award {
    bottom: 5px;
    left: 0px;
    right: 0px;
    font-size: 32px;
    animation: pop-in 1s;
  }
  .award-icon {
    height: 36px;
  }
  .time-style {
    font-weight: bold;
  }
  .award-info-icon {
    width: 30px;
  }
  .skills-color-gold {
    color: #fee101;
  }
  .skills-color-silver {
    color: #a7a7ad;
  }
  .skills-color-bronze {
    color: #a77044;
  }
  .skills-color-orange {
    color: #e76f51fc;
  }
</style>
