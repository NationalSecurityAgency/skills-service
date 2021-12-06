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
  <b-card>
    <div class="row">
      <div class="col">
        <span class="font-italic">Self Report:</span> <span class="text-primary">{{ selfReport }}</span>
      </div>
      <div class="col-auto">
        <span class="font-italic">Created:</span> <span class="text-primary">{{ skill.created | date }}</span> <span class="text-secondary">({{ value | timeFromNow }})</span>
      </div>
    </div>
    <div class="card mt-3">
      <div class="card-header">
        Description
      </div>
      <div class="card-body">
        <markdown-text v-if="skill.description" :text="skill.description" data-cy="importedSkillInfoDescription"/>
        <p v-else class="text-muted">
          Not Specified
        </p>
      </div>
    </div>
  </b-card>
</template>

<script>
  import MarkdownText from '../../utils/MarkdownText';

  export default {
    name: 'SkillToImportInfo',
    components: { MarkdownText },
    props: {
      skill: Object,
    },
    computed: {
      selfReport() {
        if (!this.skill.selfReportingType) {
          return 'N/A';
        }

        return (this.skill.selfReportingType === 'Approval') ? 'Requires Approval' : 'Honor System';
      },
    },
  };
</script>

<style scoped>

</style>
