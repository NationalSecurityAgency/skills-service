<template>
    <div id="skill-dependencies-display">
      <div class="columns">
        <div class="column is-full">
          <span class="title is-3">Dependencies</span>
        </div>
      </div>

      <div class="skills-bordered-component dependencies-container">
        <loading-container :is-loading="!loading.finishedAllSkills || !loading.finishedDependents">
            <div class="columns">
              <div class="column is-full">
                <skills-selector2 :options="allSkills" :selected="skills" v-on:added="skillAdded" v-on:removed="skillDeleted">
                  <template slot="dropdown-item" slot-scope="{ props }">
                    <div class="columns">
                      <div class="column is-narrow" style="width: 40px;">
                        <i v-if="props.option.otherProjectId" class="fas fa-w-16 fa-handshake"></i>
                        <i v-else class="fas fa-w-16 fa-list-alt"></i>
                      </div>
                      <div class="column skills-handle-overflow" style="width:30%;" :title="props.option.name">
                        <span class="selector-skill-name">
                          <span v-if="props.option.otherProjectId" class="has-text-weight-bold">{{props.option.otherProjectName}} : </span>
                          {{ props.option.name }}</span>
                      </div>
                      <div class="column is-one-fifth skills-handle-overflow" style="width:20%;" :title="props.option.skillId">
                        <span class="selector-other-label">ID:</span> <span class="selector-other-value">{{props.option.skillId}}</span>
                      </div>
                      <div class="column is-one-fifth" style="width:15%;">
                        <span v-if="props.option.otherProjectId" class="has-text-warning">** Shared Skill **</span>
                        <span v-else>
                          <span class="selector-other-label">Total Points:</span> <span class="selector-other-value">{{ props.option.totalPoints}}</span>
                        </span>
                      </div>
                    </div>
                  </template>

                  <template slot="selected-item" slot-scope="{ props }">
                    <span class="tag has-text-weight-bold" style="margin: 5px;" v-bind:style="{'background-color': props.option.isFromAnotherProject ? '#ffb87f' : 'lightblue'}">
                      <span class="skills-handle-overflow" style="width: 15rem;" :title="props.option.isFromAnotherProject ? props.option.projectId + ' : ' + props.option.name : props.option.name" >
                        <span v-if="props.option.isFromAnotherProject">{{ props.option.projectId | truncate(10)}} : </span>
                        {{ props.option.name }}
                      </span>
                      <button class="delete is-small" v-on:click="props.remove(props.option)"></button>
                    </span>

                  </template>

                </skills-selector2>
              </div>
            </div>

          <b-notification :active.sync="errNotification.enable" type="is-warning">
            <span style="font-size: 1.2rem;">
              <i style="font-size: 1.4rem;" class="fa fa-exclamation-circle"></i> <strong>Error!</strong> Request could not be completed! <strong>{{ errNotification.msg }}</strong>
            </span>
          </b-notification>

          <dependants-graph :skill="skill" :dependent-skills="skills" :graph="graph"></dependants-graph>

          <simple-skills-table :skills="skills" v-on:skill-removed="skillDeleted">
            <span slot="name-cell" slot-scope="row">
              <i v-if="row.props.isFromAnotherProject" class="fas fa-w-16 fa-handshake"></i>
              <i v-else class="fas fa-w-16 fa-list-alt"></i>
              <span v-if="row.props.isFromAnotherProject" class=""><span class="has-text-weight-bold">Cross Project Dependency </span> {{row.props.projectId}} : </span>
              {{ row.props.name }}
            </span>

            <!--<b-tooltip label="By default only skills under current subject will be considered."-->
                       <!--position="is-left" animanted="true" type="is-light">-->
              <!--<span><i class="fas fa-question-circle"></i></span>-->
            <!--</b-tooltip>-->

          </simple-skills-table>

        </loading-container>
      </div>
    </div>
</template>

<script>
  import SkillsService from '../SkillsService';
  import LoadingContainer from '../../utils/LoadingContainer';
  import DependantsGraph from './DependantsGraph';
  import SkillsSelector2 from '../SkillsSelector2';
  import SimpleSkillsTable from '../SimpleSkillsTable';

  export default {
    name: 'SkillDependencies',
    components: {
      SimpleSkillsTable, SkillsSelector2, DependantsGraph, LoadingContainer,
    },
    props: ['skill'],
    data() {
      return {
        loading: {
          finishedDependents: false,
          finishedAllSkills: false,
        },
        errNotification: {
          enable: false,
          msg: '',
        },
        skills: [],
        previousSkills: [],
        graph: {},
        allSkills: [],
      };
    },
    watch: {
      // Vue caches components and when re-directed to the same component the path will be pushed
      // to the url but the component will NOT be re-mounted therefore we must listen for events and re-load
      // the data; alternatively could update
      //    <router-view :key="$route.fullPath"/>
      // but components will never get cached - caching maybe important for components that want to update
      // the url so the state can be re-build later (example include browsing a map or dependency graph in our case)
      skill: function skillChange() {
        this.initData();
      },
    },
    mounted() {
      this.initData();
    },
    methods: {
      initData() {
        this.errNotification.enable = false;
        this.loadDependentSkills();
        this.loadAllSkills();
      },
      skillDeleted(deletedItem) {
        this.loading.finishedDependents = false;
        SkillsService.removeDependency(this.skill.projectId, this.skill.skillId, deletedItem.skillId, deletedItem.projectId)
          .then(() => {
            this.errNotification.enable = false;
            this.loadDependentSkills();
          });
      },
      skillAdded(newItem) {
        this.loading.finishedDependents = false;
        SkillsService.assignDependency(this.skill.projectId, this.skill.skillId, newItem.skillId, newItem.otherProjectId)
          .then(() => {
            this.errNotification.enable = false;
            this.loadDependentSkills();
          })
          .catch((e) => {
            if (e.response.data && e.response.data.errorCode && e.response.data.errorCode === 'FailedToAssignDependency') {
              this.errNotification.msg = e.response.data.message;
              this.errNotification.enable = true;
              this.loading.finishedDependents = true;

              // force reactivity on children - copy the data trick
              // specifically so the select component removes failed item from its list
              this.skills = this.skills.map(entry => entry);
            } else {
              const errorMessage = (e.response && e.response.data && e.response.data.message) ? e.response.data.message : undefined;
              this.$router.push({ name: 'ErrorPage', query: { errorMessage } });
            }
          });
      },
      loadDependentSkills() {
        this.loading.finishedDependents = false;
        SkillsService.getDependentSkillsGraphForSkill(this.skill.projectId, this.skill.skillId)
          .then((response) => {
            this.graph = Object.assign(response, { subjectId: this.skill.subjectId });
            const myEdges = this.graph.edges.filter(entry => entry.fromId === this.skill.id);
            const myChildren = this.graph.nodes.filter(item => myEdges.find(item1 => item1.toId === item.id));
            this.skills = myChildren.map((entry) => {
              const externalProject = entry.projectId !== this.skill.projectId;
              const disableInfo = {
                manageBtn: {
                  disable: externalProject,
                  msg: 'Cannot manage skills from external projects.',
                },
              };
              return Object.assign(entry, { subjectId: this.skill.subjectId, disabledStatus: disableInfo, isFromAnotherProject: externalProject });
            });

            // this.previousSkills = this.skills.map(entry => entry);
            this.loading.finishedDependents = true;
          })
          .finally(() => {
            this.loading.finishedDependents = true;
          });
      },
      loadAllSkills() {
        this.loading.finishedAllSkills = false;
        SkillsService.getSkillsFroDependency(this.skill.projectId, this.skill.version)
          .then((skills) => {
            this.allSkills = skills.filter(item => (item.skillId !== this.skill.skillId || item.otherProjectId));
            this.loading.finishedAllSkills = true;
          })
          .finally(() => {
            this.loading.finishedAllSkills = true;
          });
      },
    },
  };
</script>

<style scoped>
  .dependencies-container {
    min-height: 800px;
  }
</style>
