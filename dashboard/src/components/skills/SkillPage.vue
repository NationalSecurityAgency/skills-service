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
  <div>
    <page-header :loading="isLoading" :options="headerOptions">
      <div slot="subSubTitle">
        <b-button @click="displayEdit"
                  size="sm"
                  variant="outline-primary" :data-cy="`editSkillButton_${this.$route.params.skillId}`"
                  :aria-label="'edit Skill '+skill.name" ref="editSkillInPlaceBtn">
          <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
        </b-button>
      </div>
    </page-header>

    <navigation :nav-items="navItems">
    </navigation>
    <edit-skill v-if="showEdit" v-model="showEdit" :skillId="skill.skillId" :is-copy="false" :is-edit="true"
                :project-id="this.$route.params.projectId" :subject-id="this.$route.params.subjectId" @skill-saved="skillEdited" @hidden="handleHide"/>
  </div>
</template>

<script>
  import { createNamespacedHelpers } from 'vuex';
  import SkillsService from './SkillsService';
  import Navigation from '../utils/Navigation';
  import PageHeader from '../utils/pages/PageHeader';
  import EditSkill from './EditSkill';

  const { mapGetters, mapActions } = createNamespacedHelpers('subjects');

  export default {
    name: 'SkillPage',
    components: {
      PageHeader,
      Navigation,
      EditSkill,
    },
    data() {
      return {
        isLoading: true,
        skill: {},
        subjectId: '',
        headerOptions: {},
        showEdit: false,
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      ...mapGetters([
        'subject',
      ]),
      navItems() {
        if (this.isLoading) {
          return [];
        }
        const items = [];
        items.push({ name: 'Overview', iconClass: 'fa-info-circle skills-color-overview', page: 'SkillOverview' });
        items.push({ name: 'Dependencies', iconClass: 'fa-project-diagram skills-color-dependencies', page: 'SkillDependencies' });
        items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SkillUsers' });
        const addEventDisabled = this.subject.totalPoints < this.$store.getters.config.minimumSubjectPoints;
        const msg = addEventDisabled ? `Subject needs at least ${this.$store.getters.config.minimumSubjectPoints} points before events can be added` : '';
        items.push({
          name: 'Add Event', iconClass: 'fa-user-plus skills-color-events', page: 'AddSkillEvent', isDisabled: addEventDisabled, msg,
        });
        items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SkillMetrics' });
        return items;
      },
    },
    watch: {
      // Vue caches components and when re-directed to the same component the path will be pushed
      // to the url but the component will NOT be re-mounted therefore we must listen for events and re-load
      // the data; alternatively could update
      //    <router-view :key="$route.fullPath"/>
      // but components will never get cached - caching maybe important for components that want to update
      // the url so the state can be re-build later (example include browsing a map or dependency graph in our case)
      '$route.params.skillId': function skillChange() {
        this.loadData();
      },
    },
    methods: {
      ...mapActions([
        'loadSubjectDetailsState',
      ]),
      displayEdit() {
        // should only enable edit button if dirty, isn't currently
        this.showEdit = true;
      },
      loadData() {
        this.isLoading = true;
        const { projectId, subjectId } = this.$route.params;
        SkillsService.getSkillDetails(this.$route.params.projectId, this.$route.params.subjectId, this.$route.params.skillId)
          .then((response) => {
            this.skill = Object.assign(response, { subjectId });
            this.headerOptions = this.buildHeaderOptions(this.skill);
            if (this.subject) {
              this.isLoading = false;
            } else {
              this.loadSubjectDetailsState({
                projectId,
                subjectId,
              }).then(() => {
                this.isLoading = false;
              });
            }
          });
      },
      skillEdited(editedSkil) {
        this.isLoading = true;
        SkillsService.saveSkill(editedSkil).then((res) => {
          const origId = this.skill.skillId;
          this.skill = Object.assign(res, { subjectId: this.$route.params.subjectId });
          if (origId !== this.skill.skillId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, skillId: this.skill.skillId } });
          }
          this.headerOptions = this.buildHeaderOptions(res);
        }).finally(() => {
          this.isLoading = false;
          this.handleFocus();
        });
      },
      handleHide(e) {
        this.showEdit = false;
        if (!e?.saved) {
          this.handleFocus();
        }
      },
      handleFocus() {
        this.$nextTick(() => {
          const ref = this.$refs.editSkillInPlaceBtn;
          if (ref) {
            ref.focus();
          }
        });
      },
      buildHeaderOptions(skill) {
        return {
          icon: 'fas fa-graduation-cap skills-color-skills',
          title: `SKILL: ${skill.name}`,
          subTitle: `ID: ${skill.skillId}`,
          stats: [{
            label: 'Points',
            count: skill.totalPoints,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }],
        };
      },
    },
  };
</script>

<style scoped>

</style>
