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
    <div class="card" data-cy="myBadges">
        <div class="card-header">
            <div class="h6 card-title mb-0 float-left text-uppercase">
                My Earned Badges
            </div>
            <span v-if="badges && badges.length > 0" class="text-muted float-right">
                <span class="badge badge-info">{{ badges.length }}</span> Badge<span v-if="badges.length > 1">s</span> Earned
            </span>
        </div>
        <div class="card-body">
            <no-data-yet v-if="!badges || badges.length === 0" title="No badges earned yet." sub-title="Take a peak at the catalog below to get started!"/>

            <div v-if="badges && badges.length > 0" class="row justify-content-md-center">
                <div v-for="(badge, index) in badges" v-bind:key="badge.badgeId" class="col-lg-3 col-sm-6 my-2">
                        <router-link  :to="badgeRouterLinkGenerator(badge)"
                                      custom
                                      :data-cy="`earnedBadgeLink_${badge.badgeId}`">
                          <div class="card h-100 skills-card-theme-border skills-navigable-item text-center">
                            <div class="card-body earned-badge" :style="{ 'margin-top': !badge.achievedWithinExpiration ? '30px' : '' }">
                              <i class="fa fa-check-circle position-absolute text-success" v-if="badge.achievementPosition > 3" style="right: 10px; top: 10px;"/>
                              <i v-if="badge.gem" class="fas fa-gem position-absolute" style="top: 10px; left: 10px; color: purple"></i>
                              <i v-if="badge.global" class="fas fa-globe position-absolute" style="top: 10px; left: 10px; color: blue"></i>

                              <span v-if="badge.achievementPosition > 0 && badge.achievementPosition <= 3" class="position-absolute user-trophy">
                                <span :class="'fa-stack fa-2x ' + classNames[badge.achievementPosition - 1]" style="vertical-align: top; font-size: 32px;">
                                  <i class="fas fa-certificate"></i>
                                  <i class="fas fa-award fa-stack-1x" style="padding-top: 5px;"></i>
                                  <span class="sr-only">You finished in </span>
                                  <span style="font-size:.4em; color:#000000;" class="fa-stack-1x">{{positionNameShort[badge.achievementPosition - 1]}}</span>
                                  <span class="sr-only"> place</span>
                                </span>
                              </span>
                              <i :class="getIconCss(badge.iconClass, index)" style="font-size: 5em;"/>
                              <div class="card-title mb-0 text-truncate">
                                  {{ badge.badge }}
                              </div>
                              <div v-if="displayBadgeProject && badge.projectName" class="text-muted text-center text-truncate" data-cy="badgeProjectName">
                                <small>Proj<span class="d-md-none d-xl-inline">ect</span>: {{badge.projectName}}</small>
                              </div>
                              <div data-cy="dateBadgeAchieved" class="text-muted mb-2"><i class="far fa-clock text-secondary" style="font-size: 0.8rem;"></i> {{ badge.dateAchieved | relativeTime() }}</div>

                              <div v-if="badge.achievedWithinExpiration" class="bonus-award mt-2 border-top">
                                <div class="award-icon"><i :class="badge.awardAttrs.iconClass + ' skills-color-orange'"></i></div>
                                <span class="sr-only">You got the </span>
                                <div style="font-size: .4em;">{{ badge.awardAttrs.name }}</div>
                                <span class="sr-only"> bonus</span>
                              </div>
                            </div>
                          </div>
                        </router-link>
                </div>
            </div>

        </div>
    </div>
</template>

<script>
  import NoDataYet from '../utilities/NoDataYet';

  export default {
    name: 'MyBadgesDetails',
    components: { NoDataYet },
    data() {
      return {
        colors: ['text-info', 'text-warning', 'text-danger', 'text-primary'],
        positionNames: ['first', 'second', 'third'],
        positionNameShort: ['1st', '2nd', '3rd'],
        classNames: ['skills-color-gold', 'skills-color-silver', 'skills-color-bronze'],
      };
    },
    props: {
      badges: {
        type: Array,
        required: true,
      },
      badgeRouterLinkGenerator: {
        type: Function,
        required: true,
      },
      displayBadgeProject: {
        type: Boolean,
        required: false,
        default: false,
      },
    },
    methods: {
      getIconCss(icon, index) {
        const colorIndex = index % this.colors.length;
        const color = this.colors[colorIndex];
        return `${icon} ${color}`;
      },
    },
  };
</script>

<style>
  @keyframes pop-in {
    0% { opacity: 0; transform: scale(0.1); }
    100% { opacity: 1; transform: scale(1); }
  }
  .earned-badge {
    cursor: pointer;
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
