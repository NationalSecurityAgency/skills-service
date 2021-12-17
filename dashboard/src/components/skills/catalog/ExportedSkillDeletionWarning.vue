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
/*
Copyright 2021 SkillTree

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
  <p>
    This will <span class="text-primary font-weight-bold">PERMANENTLY</span> remove skill <span class="text-primary font-weight-bold">[{{ skillId }}]</span> from the catalog.
    This skill is currently imported by <b-spinner v-if="loading" style="width: 1rem; height: 1rem;" variant="primary" label="Spinning" type="grow"></b-spinner><b-badge v-if="!loading" variant="info">{{ importedByNumProj }}</b-badge> projects.
  </p>

  <p>
    This action <b>CANNOT</b> be undone and will permanently remove the skill from those projects including their achievements. Please proceed with care.
  </p>
</div>
</template>

<script>
  import CatalogService from '@/components/skills/catalog/CatalogService';

  export default {
    name: 'ExportedSkillDeletionWarning',
    props: {
      skillId: String,
    },
    data() {
      return {
        loading: true,
        importedByNumProj: 0,
      };
    },
    mounted() {
      this.loading = true;
      CatalogService.getExportedStats(this.$route.params.projectId, this.skillId)
        .then((res) => {
          this.importedByNumProj = res.users ? res.users.length : 0;
        }).finally(() => {
          this.loading = false;
        });
    },
  };
</script>

<style scoped>

</style>
