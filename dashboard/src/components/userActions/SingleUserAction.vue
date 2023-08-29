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
<loading-container :is-loading="loading">
  <div v-if="attributes && Object.keys(attributes).length > 0">
    <div v-for="(value, propName) in attributes" :key="propName">
      <div v-if="propName === 'description' && value">
        <div class="font-italic">
          Description:
        </div>
        <b-card body-class="py-1" style="height:150px; overflow-y: scroll;">
        <div >
            <markdown-text v-if="value" :text="value" data-cy="descriptionText"/>
        </div>
        </b-card>
      </div>
      <div v-else class="row">
        <div class="col-3 font-italic">{{ formatLabel(propName) }}:</div>
        <div class="col my-auto">
          <span v-if="!value" class="text-secondary">Not Provided</span>
          <span v-else>
            <div v-if="isTextAreaProp(propName)"><b-textarea :value="value" :readonly="true"/></div>
            <span v-else>{{ value }}</span>
          </span>
        </div>
      </div>
    </div>
  </div>
  <div v-else>
    <i class="fas fa-dragon" aria-hidden="true"/> No additional information to show
  </div>
</loading-container>
</template>

<script>
  import MarkdownText from '@/common-components/utilities/MarkdownText';
  import UserActionsService from '@/components/userActions/UserActionsService';
  import LoadingContainer from '@/components/utils/LoadingContainer';

  const propsLookupByItem = new Map();
  propsLookupByItem.set('Skill', ['name', 'description', 'groupId', 'helpUrl', 'pointIncrement', 'numMaxOccurrencesIncrementInterval', 'pointIncrementInterval', 'selfReportingType', 'version']);
  propsLookupByItem.set('Subject', ['name', 'description', 'helpUrl', 'iconClass']);
  propsLookupByItem.set('Project', ['name', 'projectId', 'description']);
  propsLookupByItem.set('Level', ['level', 'percent', 'pointsTo', 'pointsFrom']);
  const excludeLookupFromActions = ['Move', 'ImportFromCatalog', 'ReuseInProject'];
  const valuesMap = new Map();
  valuesMap.set('production.mode.enabled', 'Is in the Project Catalog');
  valuesMap.set('invite_only', 'Is Private Invite Only Project');
  valuesMap.set('project-admins_rank_and_leaderboard_optOut', 'Ranking and Leaderboard Opt-Out');
  valuesMap.set('level.points.enabled', 'Use Points For Levels');
  valuesMap.set('selfReport.type', 'Self Report Type');
  valuesMap.set('help.url.root', 'Root Help URL');
  valuesMap.set('group-descriptions', 'Always Show Group Descriptions');
  valuesMap.set('show_project_description_everywhere', 'Show Project Description everywhere');
  export default {
    name: 'SingleUserAction',
    components: { MarkdownText, LoadingContainer },
    props: {
      actionId: Number,
      item: String,
      action: String,
    },
    data() {
      return {
        loading: true,
        attributes: {},
      };
    },
    mounted() {
      this.loadData();
    },
    methods: {
      loadData() {
        this.loading = true;
        UserActionsService.getDashboardSingleAction(this.actionId)
          .then((res) => {
            const propsToShow = propsLookupByItem.get(this.item);
            if (!excludeLookupFromActions.includes(this.action) && propsToShow) {
              this.attributes = Object.fromEntries(Object.entries(res)
                .filter(([key]) => propsToShow.includes(key)));
            } else {
              this.attributes = res;
            }
            this.attributes = Object.fromEntries(Object.entries(res).map((entry) => {
              console.log(entry);
              const replacement = valuesMap.get(entry[1]);
              if (replacement) {
                return [entry[0], replacement];
              }
              return entry;
            }));
          }).finally(() => {
            this.loading = false;
          });
      },
      formatLabel(originalLabel) {
        return originalLabel
          .replace(/([A-Z])/g, (match) => ` ${match}`)
          .replace(/^./, (match) => match.toUpperCase());
      },
      isTextAreaProp(propName) {
        return propName === 'transcript' || propName === 'captions';
      },
    },
  };
</script>

<style scoped>

</style>
