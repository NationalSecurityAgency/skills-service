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
                    <i v-if="badge.gem" class="fas fa-gem position-absolute" style="top: 5px; right: 5px; color: purple"></i>
                    <i v-if="badge.global" class="fas fa-globe position-absolute" style="top: 5px; right: 5px; color: blue"></i>

                    <span v-if="badge.achievementPosition > 0 && badge.achievementPosition <= 3" class="position-absolute user-trophy">
                      <span :class="'fa-stack fa-2x ' + classNames[badge.achievementPosition - 1]" style="vertical-align: top; font-size:32px;">
                        <i class="fas fa-certificate"></i>
                        <i class="fas fa-ribbon fa-stack-1x" style="padding-top: 5px;"></i>
                        <span style="font-size:.4em; color:#000000;" class="fa-stack-1x">{{positionNameShort[badge.achievementPosition - 1]}}</span>
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

                    <div v-if="badge.achievedWithinExpiration" class="bonus-award">
                      <div class="award-icon"><i :class="badge.awardAttrs.iconClass + ' skills-color-orange'"></i></div>
                      <div style="font-size: .4em;">{{ badge.awardAttrs.name }}</div>
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

            <div class="alert alert-success" v-if="badge">
              <div v-if="badge.numberOfUsersAchieved > 0">
                <i class="fas fa-trophy" style="padding-right: 10px;"></i>
                <span v-if="!badge.badgeAchieved">{{badge.numberOfUsersAchieved}} {{usersAchieved}} achieved this badge so far - you could be next!</span>
                <span v-else-if="badge.badgeAchieved && badge.numberOfUsersAchieved > 1">{{badge.numberOfUsersAchieved - 1}} other {{otherUsersAchieved}} achieved this badge so far</span>
                <span v-else>You've achieved this badge</span>
                <span v-if="achievementOrder !== ''"> - and you were the {{achievementOrder}}!</span>
              </div>
              <div v-else><i class="fas fa-car-side" style="padding-right: 10px;"></i> No one has achieved this badge yet - you could be the first!</div>

              <div v-if="badge.firstPerformedSkill && !badge.badgeAchieved">
                <i class="fas fa-clock" style="padding-right: 10px;"></i> You started working on this badge <span :title="badge.firstPerformedSkill">{{ badge.firstPerformedSkill | relativeTime() }}</span>.
                <span v-if="!badge.hasExpired && badge.expirationDate">
                   Achieve it {{ badge.expirationDate | relativeTime() }} for a bonus!
                </span>
              </div>

              <div v-if="badge.badgeAchieved && badge.achievedWithinExpiration">
                <i :class="badge.awardAttrs.iconClass" style="padding-right: 7px;"></i> You've earned the {{ badge.awardAttrs.name }} bonus!
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
      };
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
      numberOfStars() {
        let numberOfStars = 0;
        if (this.badge.achievementPosition === 1) {
          numberOfStars = 3;
        } else if (this.badge.achievementPosition === 2) {
          numberOfStars = 2;
        } else if (this.badge.achievementPosition === 3) {
          numberOfStars = 1;
        }
        return numberOfStars;
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
</style>
