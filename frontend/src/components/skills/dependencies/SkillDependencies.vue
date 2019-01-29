<template>
    <div>
      <loading-container :is-loading="!loading.finishedAllSkills || !loading.finishedDependents">
        <!--<dependency-graph :project-id="this.skill.projectId"></dependency-graph>-->

        <!--<hr/>-->

        <div class="columns">
          <div class="column is-full">
            <span class="title is-3">Dependencies</span>
          </div>
        </div>

        <div class="skills-bordered-component">
          <div class="columns">
            <div class="column is-full">
              <skills-selector v-model="skills" :available-to-select="allSkills" v-on:selection-changed="selectionChanged"></skills-selector>
            </div>
          </div>

          <dependants-graph :skill="skill" :dependent-skills="skills" :graph="graph"></dependants-graph>

          <dependents-table :skills="skills" v-on:skill-removed="skillDeleted"></dependents-table>
        </div>

      </loading-container>
    </div>
</template>

<script>
  import SkillsService from '../SkillsService';
  import SkillsTable from '../SkillsTable';
  import SkillsSelector from '../SkillsSelector';
  import LoadingContainer from '../../utils/LoadingContainer';
  import DependentsTable from './DependentsTable';
  import DependantsGraph from './DependantsGraph';

  export default {
    name: 'SkillDependencies',
    components: { DependantsGraph, DependentsTable, LoadingContainer, SkillsTable, SkillsSelector },
    props: ['skill'],
    data() {
      return {
        loading: {
          finishedDependents: false,
          finishedAllSkills: false,
        },
        skills: [],
        previousSkills: [],
        graph: {},
        allSkills: [],
        serverErrors: [],
      };
    },
    mounted() {
      this.loadDependentSkills();
      this.loadAllSkills();
    },
    methods: {
      selectionChanged() {
        const newItems = this.skills.filter(item => !this.previousSkills.find(item1 => item1.id === item.id));
        newItems.forEach((newItem) => {
          SkillsService.assignDependency(this.skill.projectId, this.skill.skillId, newItem.skillId);
          Object.assign(newItem, { subjectId: this.skill.subjectId });
        });

        const deletedItems = this.previousSkills.filter(item => !this.skills.find(item1 => item1.id === item.id));
        deletedItems.forEach((deletedItem) => {
          this.skillDeleted(deletedItem);
        });
      },
      skillDeleted(skill) {
        SkillsService.removeDependency(this.skill.projectId, this.skill.skillId, skill.skillId)
          .then(() => {
            this.skills = this.skills.filter(item => item.id !== skill.id);
            this.previousSkills = this.skills.map(entry => entry);
          })
          .catch((e) => {
            this.serverErrors.push(e);
            throw e;
        });
      },
      loadDependentSkills() {
        SkillsService.getDependentSkillsGraphForSkill(this.skill.projectId, this.skill.skillId)
          .then((response) => {
            // this.skills = response.map(entry => Object.assign(entry, { subjectId: this.skill.subjectId }));
            // this.previousSkills = this.skills.map(entry => entry);
            this.graph = Object.assign(response, { subjectId: this.skill.subjectId });
            const myEdges = this.graph.edges.filter(entry => entry.fromId === this.skill.id);
            const myChildren = this.graph.nodes.filter(item => myEdges.find(item1 => item1.toId === item.id));
            this.skills = myChildren.map(entry => Object.assign(entry, { subjectId: this.skill.subjectId }));
            this.previousSkills = this.skills.map(entry => entry);
            this.loading.finishedDependents = true;
          })
          .catch((e) => {
            this.serverErrors.push(e);
            this.loading.finishedDependents = true;
            throw e;
        });
      },
      loadAllSkills() {
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

</style>
