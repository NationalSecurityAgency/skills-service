<template>
  <div>
    <sub-page-header title="Dependencies"/>

    <simple-card class="dependencies-container">
      <loading-container :is-loading="!loading.finishedAllSkills || !loading.finishedDependents">
        <skills-selector2 :options="allSkills" :selected="skills" v-on:added="skillAdded" v-on:removed="skillDeleted">
          <template slot="dropdown-item" slot-scope="{ props }">
            <div class="media">
              <div class="d-inline-block mt-1 mr-3">
                <i v-if="props.option.otherProjectId" class="fas fa-w-16 fa-handshake text-primary"></i>
                <i v-else class="fas fa-w-16 fa-list-alt text-info"></i>
              </div>
              <div class="media-body">
                <strong class="mb-2"><span v-if="props.option.otherProjectId" class="">{{props.option.otherProjectName}} : </span>
                  {{ props.option.name }}</strong>
                <div style="font-size: 0.95rem;" class="text-secondary">
                  <span class="">ID:</span> <span class="">{{props.option.skillId}}</span>
                  <span v-if="props.option.otherProjectId" class="text-warning ml-3">** Shared Skill **</span>
                </div>
              </div>
            </div>
          </template>

          <template slot="selected-item" slot-scope="{ props }">
            <span class="mt-2 mr-2 border-primary rounded px-1" style="padding-top: 2px; padding-bottom: 2px;"
                  v-bind:style="{'background-color': props.option.isFromAnotherProject ? '#ffb87f' : 'lightblue'}">
              <span class="skills-handle-overflow" style="width: 15rem;"
                    :title="props.option.isFromAnotherProject ? props.option.projectId + ' : ' + props.option.name : props.option.name">
                <span v-if="props.option.isFromAnotherProject">{{ props.option.projectId | truncate(10)}} : </span>
                {{ props.option.name }}
              </span>
              <button class="btn btn-sm btn-outline-secondary p-0 border-0 ml-1"
                      v-on:click="props.remove(props.option)"><i class="fas fa-times"/></button>
            </span>
          </template>
        </skills-selector2>

        <b-alert v-if="errNotification.enable" variant="danger" class="mt-2" show dismissible>
          <i class="fa fa-exclamation-circle mr-1"></i> <strong>Error!</strong> Request could not be completed! <strong>{{
          errNotification.msg }}</strong>
        </b-alert>

        <dependants-graph :skill="skill" :dependent-skills="skills" :graph="graph" class="my-3"/>

        <simple-skills-table :skills="skills" v-on:skill-removed="skillDeleted">
            <span slot="name-cell" slot-scope="row">
              <i v-if="row.props.isFromAnotherProject" class="fas fa-w-16 fa-handshake text-primary mr-1"></i>
              <i v-else class="fas fa-w-16 fa-list-alt text-info mr-1"></i>
              {{ row.props.name }}
               <div v-if="row.props.isFromAnotherProject" class="">
                Cross Project Dependency from project [{{row.props.projectId}}]
              </div>
            </span>
        </simple-skills-table>

      </loading-container>
    </simple-card>
  </div>
</template>

<script>
  import SkillsService from '../SkillsService';
  import DependantsGraph from './DependantsGraph';
  import SkillsSelector2 from '../SkillsSelector2';
  import SimpleSkillsTable from '../SimpleSkillsTable';
  import SubPageHeader from '../../utils/pages/SubPageHeader';
  import SimpleCard from '../../utils/cards/SimpleCard';
  import LoadingContainer from '../../utils/LoadingContainer';

  export default {
    name: 'SkillDependencies',
    components: {
      LoadingContainer,
      SimpleCard,
      SubPageHeader,
      SimpleSkillsTable,
      SkillsSelector2,
      DependantsGraph,
    },
    data() {
      return {
        skill: {},
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
      this.loadSkill();
    },
    methods: {
      initData() {
        this.errNotification.enable = false;
        this.loadAllSkills();
        this.loadDependentSkills();
      },
      loadSkill() {
        SkillsService.getSkillDetails(this.$route.params.projectId, this.$route.params.subjectId, this.$route.params.skillId)
          .then((response) => {
            this.skill = Object.assign(response, { subjectId: this.$route.params.subjectId });
          });
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
              return Object.assign(entry, {
                subjectId: this.skill.subjectId,
                disabledStatus: disableInfo,
                isFromAnotherProject: externalProject,
              });
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
