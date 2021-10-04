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
  <loading-container :is-loading="loading" class="container-fluid">

    <sub-page-header title="My Projects" class="pt-4">
        <b-button id="manageMyProjectsBtn" :to="{ name: 'DiscoverProjectsPage' }" variant="outline-primary" data-cy="manageMyProjsBtn"><i class="fas fa-cog" aria-hidden="true"/> Manage My Projects</b-button>
      <b-tooltip v-if="!hasProjects" target="manageMyProjectsBtn" placement="bottom" variant="primary">
        <i class="fas fa-info-circle"></i> Click here to add to
        <div class="text-uppercase"><b>My Projects</b></div>
      </b-tooltip>
    </sub-page-header>

      <no-content2 v-if="!hasProjects" class="mt-5 pb-4"
                   title="START CUSTOMIZING TODAY!"
                   icon="fas fa-user-cog" icon-color="text-info" icon-size="fa-4x">
        <b-card class="mb-5 mt-2 px-5">
          <div>Please click</div>
          <div class="my-2">
          <b-button :to="{ name: 'DiscoverProjectsPage' }" variant="outline-primary" data-cy="manageMyProjsBtnInNoContent" class="animate__bounceIn"><i class="fas fa-cog" aria-hidden="true"/> Manage My Projects</b-button>
          </div>
          <div>
          on the <b>top-right</b> to start adding adding projects to <b class="text-uppercase">My Projects</b> view.
          </div>
        </b-card>
      </no-content2>

    <div v-if="!loading && hasProjects">
      <b-row class="my-4">
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pl-md-3 pr-md-1">
          <info-snapshot-card :projects="myProjects"
                              :num-projects-contributed="myProgress.numProjectsContributed"
                              class="flex-grow-1 my-summary-card"/>
        </b-col>
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pr-md-3 pl-md-1 pr-xl-1">
          <num-skills :total-skills="myProgress.totalSkills"
                      :num-achieved-skills="myProgress.numAchievedSkills" class="flex-grow-1 my-summary-card"/>
        </b-col>
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pl-md-3 pr-md-1 pl-xl-1">
          <last-earned-card :num-achieved-skills-last-month="myProgress.numAchievedSkillsLastMonth"
                            :num-achieved-skills-last-week="myProgress.numAchievedSkillsLastWeek"
                            :most-recent-achieved-skill="myProgress.mostRecentAchievedSkill"
                            class="flex-grow-1 my-summary-card"/>
        </b-col>
        <b-col cols="12" md="6" xl="3" class="d-flex mb-2 pr-md-3 pl-md-1">
          <badges-num-card :total-badges="myProgress.totalBadges"
                           :num-achieved-badges="myProgress.numAchievedBadges"
                           :num-achieved-gem-badges="myProgress.numAchievedGemBadges"
                           :num-achieved-global-badges="myProgress.numAchievedGlobalBadges"
                           :total-gems="myProgress.gemCount"
                           :total-global-badges="myProgress.globalBadgeCount"
                           class="flex-grow-1 my-summary-card"/>
        </b-col>
      </b-row>
      <hr/>
      <b-row class="my-4 px-1" id="projectCards">
        <b-col v-for="(proj) in myProjects" :key="proj.projectName" :id="proj.projectId"
               cols="12" md="6" xl="4"
               class="mb-2 px-2">
          <b-overlay :show="sortOrderLoading" rounded="sm" opacity="0.4">
            <template #overlay>
              <div class="text-center">
                <div v-if="proj.projectId===sortOrderLoadingProjectId">
                  <div class="text-info text-uppercase mb-1">Updating sort order!</div>
                  <b-spinner label="Loading..." style="width: 3rem; height: 3rem;" variant="info"/>
                </div>
              </div>
            </template>

            <router-link :to="{ name:'MyProjectSkills', params: { projectId: proj.projectId, name: proj.projectName } }" tag="div"
                         class="project-link" :data-cy="`project-link-${proj.projectId}`">
              <project-link-card :proj="proj" class="my-summary-card" />
            </router-link>
          </b-overlay>
        </b-col>
      </b-row>
    </div>
  </loading-container>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import Sortable from 'sortablejs';
  import NoContent2 from '@/components/utils/NoContent2';
  import ProjectLinkCard from './ProjectLinkCard';
  import InfoSnapshotCard from './InfoSnapshotCard';
  import NumSkills from './NumSkills';
  import BadgesNumCard from './BadgesNumCard';
  import LastEarnedCard from './LastEarnedCard';
  import LoadingContainer from '../utils/LoadingContainer';
  import SubPageHeader from '../utils/pages/SubPageHeader';
  import ProjectService from '../projects/ProjectService';

  const { mapActions, mapGetters } = createNamespacedHelpers('myProgress');

  export default {
    name: 'MyProgressPage',
    components: {
      SubPageHeader,
      NoContent2,
      LastEarnedCard,
      BadgesNumCard,
      NumSkills,
      InfoSnapshotCard,
      ProjectLinkCard,
      LoadingContainer,
    },
    data() {
      return {
        loading: true,
        sortOrderLoading: false,
        sortOrderLoadingProjectId: -1,
        projects: [],
      };
    },
    mounted() {
      this.loadMyProgressSummary().finally(() => {
        this.loading = false;
        this.enableProjectDropAndDrop();
      });
    },
    methods: {
      ...mapActions(['loadMyProgressSummary']),
      enableProjectDropAndDrop() {
        if (this.myProjects && this.myProjects.length > 0) {
          const self = this;
          this.$nextTick(() => {
            const cards = document.getElementById('projectCards');
            Sortable.create(cards, {
              handle: '.sort-control',
              animation: 150,
              ghostClass: 'skills-sort-order-ghost-class',
              onUpdate(event) {
                self.projectOrderUpdate(event);
              },
            });
          });
        }
      },
      projectOrderUpdate(updateEvent) {
        const projectId = updateEvent.item.id;
        this.sortOrderLoadingProjectId = projectId;
        this.sortOrderLoading = true;
        ProjectService.moveMyProject(projectId, updateEvent.newIndex)
          .finally(() => {
            this.sortOrderLoading = false;
          });
      },
    },
    computed: {
      hasProjects() {
        return this.myProjects && this.myProjects.length > 0;
      },
      ...mapGetters([
        'myProgress',
        'myProjects',
      ]),
    },
  };
</script>

<style scoped>
.project-link :hover {
  cursor: pointer;
}
.my-summary-card {
  min-width: 17rem !important;
}
hr {
  border:none;
  height: 20px;
  width: 90%;
  height: 50px;
  margin-top: 0;
  border-bottom: 1px solid rgba(45, 135, 121, 0.31);
  box-shadow: 0 10px 10px -10px rgba(45, 134, 120, 0.15);
  margin: -50px auto 10px;
  width: 90%;
}

</style>
