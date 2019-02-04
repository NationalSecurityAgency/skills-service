<template>
    <div>
      <div class="columns">
        <div class="column is-full">
          <span class="title is-3">Dependencies</span>
        </div>
      </div>

      <div class="skills-bordered-component dependencies-container">
        <loading-container :is-loading="!loading.finishedAllSkills || !loading.finishedDependents">
            <div class="columns">
              <div class="column is-full">
                <skills-selector2 :options="allSkills" :selected="skills" v-on:added="skillAdded" v-on:removed="skillDeleted"></skills-selector2>
              </div>
            </div>

          <b-notification :active.sync="errNotification.enable" type="is-warning">
            <span style="font-size: 1.2rem;">
              <i style="font-size: 1.4rem;" class="fa fa-exclamation-circle"></i> <strong>Error!</strong> Request could not be completed! <strong>{{ errNotification.msg }}</strong>
            </span>
          </b-notification>

          <dependants-graph :skill="skill" :dependent-skills="skills" :graph="graph"></dependants-graph>

          <simple-skills-table :skills="skills" v-on:skill-removed="skillDeleted"></simple-skills-table>

        </loading-container>
      </div>
    </div>
</template>

<script>
  import SkillsService from '../SkillsService';
  import SkillsTable from '../SkillsTable';
  import SkillsSelector from '../SkillsSelector';
  import LoadingContainer from '../../utils/LoadingContainer';
  import DependantsGraph from './DependantsGraph';
  import SkillsSelector2 from '../SkillsSelector2';
  import SimpleSkillsTable from '../SimpleSkillsTable';

  export default {
    name: 'SkillDependencies',
    components: { SimpleSkillsTable, SkillsSelector2, DependantsGraph, LoadingContainer, SkillsTable, SkillsSelector },
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
        serverErrors: [],
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
        SkillsService.removeDependency(this.skill.projectId, this.skill.skillId, deletedItem.skillId)
          .then(() => {
            this.errNotification.enable = false;
            this.loadDependentSkills();
          })
          .catch((e) => {
            this.serverErrors.push(e);
            throw e;
        });
      },
      skillAdded(newItem) {
        SkillsService.assignDependency(this.skill.projectId, this.skill.skillId, newItem.skillId)
          .then(() => {
            this.errNotification.enable = false;
            this.loadDependentSkills();
          })
          .catch((e) => {
            if (e.response.data && e.response.data.errCode && e.response.data.errCode === 'FailedToAssignDependency') {
              this.errNotification.msg = e.response.data.errorMsg;
              this.errNotification.enable = true;

              // force reactivity on children - copy the data trick
              // specifically so the select component removes failed item from its list
              this.skills = this.skills.map(entry => entry);
            } else {
              this.serverErrors.push(e);
              throw e;
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
            this.skills = myChildren.map(entry => Object.assign(entry, { subjectId: this.skill.subjectId }));

            // this.previousSkills = this.skills.map(entry => entry);
            this.loading.finishedDependents = true;
          })
          .catch((e) => {
            this.serverErrors.push(e);
            this.loading.finishedDependents = true;
            throw e;
        });
      },
      loadAllSkills() {
        this.loading.finishedAllSkills = false;
        SkillsService.getProjectSkills(this.skill.projectId)
          .then((skills) => {
            this.allSkills = skills.filter(item => item.id !== this.skill.id);
            this.loading.finishedAllSkills = true;
          })
          .catch((e) => {
            this.serverErrors.push(e);
            this.loading.finishedAllSkills = true;
            throw e;
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
