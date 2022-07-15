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
      <exported-skill-deletion-warning :loaded-stats="loadedStats"
                                       :skill-name="skillToRemove.skillName"/>
    </div>
  </div>
</template>

<script>
  import ExportedSkillDeletionWarning
    from '@/components/skills/catalog/ExportedSkillDeletionWarning';
  import CatalogService from '@/components/skills/catalog/CatalogService';
  import SkillsSpinner from '@/components/utils/SkillsSpinner';

  export default {
    name: 'ExportedSkillRemovalValidation',
    components: {
      SkillsSpinner,
      ExportedSkillDeletionWarning,
    },
    props: {
      skillToRemove: Object,
    },
    data() {
      return {
        loading: true,
        loadedStats: null,
      };
    },
    mounted() {
      this.loading = true;
      CatalogService.getExportedStats(this.$route.params.projectId, this.skillToRemove.skillId)
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
