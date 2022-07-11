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
    <skills-spinner :is-loading="loading" class="mb-5"/>
    <div v-if="!loading">
      <p>
        This will remove <span
        class="text-primary font-weight-bold">{{ deleteSkillInfo.skill.name }}</span>.
        <span v-if="deleteSkillInfo.skill.reusedSkill">The skill is <b-badge variant="success"
                                                                             class="text-uppercase"><i
          class="fas fa-recycle"></i> reused</b-badge> and this action will <b>only</b> remove the reused skill, and not the original!</span>
      </p>
      <div v-if="deleteSkillInfo.skill.isSkillType">
        Delete Action <b class="text-danger">CANNOT</b> be undone and permanently removes users'
        performed skills and any dependency associations.
      </div>
      <div v-if="deleteSkillInfo.skill.isGroupType">
        Delete Action <b class="text-danger">CANNOT</b> be undone and will permanently remove all of
        the group's skills. All the associated users' performed skills and any dependency
        associations will also be removed.
      </div>
      <div v-if="loadedStats.isExported" class="alert alert-info mt-3">
        <exported-skill-deletion-warning :loaded-stats="loadedStats"
                                         :skill-name="deleteSkillInfo.skill.name"/>
      </div>
      <div v-if="loadedStats.isReusedLocally" class="alert alert-info mt-3">
        Please note that the skill is currently
        <b-badge variant="success" class="text-uppercase"><i class="fas fa-recycle"></i> reused
        </b-badge>
        in this project.
        Deleting this skill will also remove its reused copies.
      </div>
    </div>
  </div>
</template>

<script>
  import ExportedSkillDeletionWarning
    from '@/components/skills/catalog/ExportedSkillDeletionWarning';
  import CatalogService from '@/components/skills/catalog/CatalogService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'SkillRemovalValidation',
    components: {
      SkillsSpinner,
      ExportedSkillDeletionWarning,
    },
    props: {
      deleteSkillInfo: Object,
    },
    data() {
      return {
        loading: true,
        loadedStats: null,
      };
    },
    mounted() {
      this.loading = true;
      CatalogService.getExportedStats(this.$route.params.projectId, this.deleteSkillInfo.skill.skillId)
        .then((res) => {
          this.loadedStats = res;
        })
        .finally(() => {
          this.loading = false;
        });
    },
  };
</script>

<style scoped>

</style>
