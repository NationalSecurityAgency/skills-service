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
      <div slot="subTitle" v-if="skill">
        <div v-for="(tag) in skill.tags" :key="tag.tagId" class="h6 mr-2 d-inline-block" :data-cy="`skillTag-${skill.skillId}-${tag.tagId}`">
          <b-badge variant="info">
            <span><i class="fas fa-tag"></i> {{ tag.tagValue }}</span>
          </b-badge>
        </div>
        <div class="h5 text-muted" data-cy="skillId">
          <show-more :limit="54" :text="getSkillId(skill)"></show-more>
        </div>
        <div class="h5 text-muted" v-if="skill && skill.groupId">
          <span style="font-size: 1rem">Group ID:</span> <span v-b-tooltip.hover="`Name: ${ skill.groupName }`">{{ skill.groupId }}</span>
        </div>
      </div>
      <div slot="subSubTitle" v-if="!isImported">
        <b-button-group>
          <b-button v-if="skill && projConfig && !isReadOnlyProj" @click="displayEdit"
                    size="sm"
                    variant="outline-primary" :data-cy="`editSkillButton_${this.$route.params.skillId}`"
                    :aria-label="'edit Skill '+skill.name" ref="editSkillInPlaceBtn">
            <span class="d-none d-sm-inline">Edit </span> <i class="fas fa-edit" aria-hidden="true"/>
          </b-button>
        </b-button-group>
      </div>
      <div slot="right-of-header" v-if="!isLoading && (skill.sharedToCatalog || isImported)"
           class="d-inline h5">
        <b-badge v-if="skill.sharedToCatalog" class="ml-2" data-cy="exportedBadge"><i
          class="fas fa-book"></i> EXPORTED
        </b-badge>
        <b-badge v-if="isImported" class="ml-2" variant="success" data-cy="importedBadge">
          <span v-if="skill.reusedSkill"><i class="fas fa-recycle"></i> Reused</span>
          <span v-else><i class="fas fa-book"></i> IMPORTED</span>
        </b-badge>
        <b-badge v-if="!skill.enabled" class="ml-2" data-cy="disabledSkillBadge"> DISABLED</b-badge>
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
  import SkillReuseIdUtil from '@/components/utils/SkillReuseIdUtil';
  import SkillsService from '@/components/skills/SkillsService';
  import Navigation from '@/components/utils/Navigation';
  import PageHeader from '@/components/utils/pages/PageHeader';
  import EditSkill from '@/components/skills/EditSkill';
  import ShowMore from '@/components/skills/selfReport/ShowMore';
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';

  const subjects = createNamespacedHelpers('subjects');
  const skills = createNamespacedHelpers('skills');

  export default {
    name: 'SkillPage',
    mixins: [ProjConfigMixin],
    components: {
      PageHeader,
      Navigation,
      EditSkill,
      ShowMore,
    },
    data() {
      return {
        isLoadingData: true,
        subjectId: '',
        headerOptions: {},
        showEdit: false,
      };
    },
    mounted() {
      this.loadData();
    },
    computed: {
      ...subjects.mapGetters([
        'subject',
      ]),
      ...skills.mapGetters([
        'skill',
      ]),
      isLoading() {
        return this.isLoadingData || this.isLoadingProjConfig;
      },
      navItems() {
        if (this.isLoadingData) {
          return [];
        }
        const items = [];
        items.push({ name: 'Overview', iconClass: 'fa-info-circle skills-color-overview', page: 'SkillOverview' });
        if (!this.isImported) {
          items.push({ name: 'Dependencies', iconClass: 'fa-project-diagram skills-color-dependencies', page: 'SkillDependencies' });
        }
        items.push({ name: 'Users', iconClass: 'fa-users skills-color-users', page: 'SkillUsers' });
        const isReadOnlyNonSr = (this.skill.readOnly === true && !this.skill.selfReportType);
        const addEventDisabled = this.subject.totalPoints < this.$store.getters.config.minimumSubjectPoints || isReadOnlyNonSr;

        let msg = addEventDisabled ? `Subject needs at least ${this.$store.getters.config.minimumSubjectPoints} points before events can be added` : '';
        const disabledDueToGroupBeingDisabled = this.skill.groupId && !this.skill.enabled;
        if (disabledDueToGroupBeingDisabled) {
          msg = `CANNOT report skill events because this skill belongs to a group whose current status is disabled. ${msg}`;
        }
        if (isReadOnlyNonSr) {
          msg = 'Skills imported from the catalog can only have events added if they are configured for Self Reporting';
        }
        if (!this.isImported && !this.isReadOnlyProj) {
          items.push({
            name: 'Add Event', iconClass: 'fa-user-plus skills-color-events', page: 'AddSkillEvent', isDisabled: addEventDisabled || disabledDueToGroupBeingDisabled || isReadOnlyNonSr, msg,
          });
        }
        items.push({ name: 'Metrics', iconClass: 'fa-chart-bar skills-color-metrics', page: 'SkillMetrics' });
        return items;
      },
      isImported() {
        return this.skill && this.skill.copiedFromProjectId && this.skill.copiedFromProjectId.length > 0;
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
      ...subjects.mapActions([
        'loadSubjectDetailsState',
      ]),
      ...skills.mapActions([
        'loadSkill',
      ]),
      ...skills.mapMutations([
        'setSkill',
      ]),
      displayEdit() {
        // should only enable edit button if dirty, isn't currently
        this.showEdit = true;
      },
      loadData() {
        this.isLoadingData = true;
        const { projectId, subjectId } = this.$route.params;
        this.loadSkill({
          projectId: this.$route.params.projectId,
          subjectId: this.$route.params.subjectId,
          skillId: this.$route.params.skillId,
        }).then(() => {
          this.headerOptions = this.buildHeaderOptions(this.skill);
          if (this.subject) {
            this.isLoadingData = false;
          } else {
            this.loadSubjectDetailsState({
              projectId,
              subjectId,
            }).then(() => {
              this.isLoadingData = false;
            });
          }
        });
      },
      skillEdited(editedSkil) {
        this.isLoadingData = true;
        SkillsService.saveSkill(editedSkil).then((res) => {
          const origId = this.skill.skillId;
          const edited = Object.assign(res, { subjectId: this.$route.params.subjectId });
          this.setSkill(edited);
          if (origId !== this.skill.skillId) {
            this.$router.replace({ name: this.$route.name, params: { ...this.$route.params, skillId: this.skill.skillId } });
          }
          this.headerOptions = this.buildHeaderOptions(res);
        }).then(() => {
          this.$nextTick(() => this.$announcer.polite(`Skill ${editedSkil.name} has been edited`));
        })
        .catch((err) => {
          if (err && err.response && err.response.data.errorCode === 'MaxSkillsThreshold') {
            this.msgOk(err.response.data.explanation, 'Maximum Skills Reached');
          } else if (err?.response?.data?.errorCode === 'DbUpgradeInProgress') {
            this.$router.push({ name: 'DbUpgradeInProgressPage' });
          } else {
            throw err;
          }
        })
        .finally(() => {
          this.isLoadingData = false;
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
        const skillId = skill?.skillId ? SkillReuseIdUtil.removeTag(skill.skillId) : '';
        return {
          icon: 'fas fa-graduation-cap skills-color-skills',
          title: `SKILL: ${skill?.name}`,
          subTitle: `ID: ${skillId} | GROUP ID: ${skill?.groupId}`,
          stats: [{
            label: 'Points',
            count: skill?.totalPoints,
            icon: 'far fa-arrow-alt-circle-up skills-color-points',
          }],
        };
      },
      getSkillId(skill) {
        return skill ? `ID: ${SkillReuseIdUtil.removeTag(skill.skillId)}` : 'Loading...';
      },
    },
  };
</script>

<style scoped>

</style>
