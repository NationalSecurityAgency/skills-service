/*
Copyright 2024 SkillTree

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
<script setup>

import { onMounted, ref } from 'vue';
import { useRoute } from 'vue-router';
import UserActionsService from '@/components/userActions/UserActionsService.js';
import LoadingContainer from '@/components/utils/LoadingContainer.vue'
import MarkdownText from '@/common-components/utilities/markdown/MarkdownText.vue';

const props = defineProps({
  actionId: Number,
  item: String,
  action: String,
})

const route = useRoute()

const loading = ref(true)
const attributes = ref([])

const propsLookupByItem = new Map();
propsLookupByItem.set('Skill', ['name', 'description', 'groupId', 'helpUrl', 'pointIncrement', 'numMaxOccurrencesIncrementInterval', 'pointIncrementInterval', 'selfReportingType', 'version', 'justificationRequired', 'totalPoints', 'skillId']);
propsLookupByItem.set('Subject', ['name', 'description', 'helpUrl', 'iconClass']);
propsLookupByItem.set('SkillsGroup', ['name', 'description', 'helpUrl']);
propsLookupByItem.set('Project', ['name', 'projectId', 'description']);
propsLookupByItem.set('Level', ['level', 'percent', 'pointsTo', 'pointsFrom']);
propsLookupByItem.set('Quiz', ['name', 'description', 'type']);
propsLookupByItem.set('Badge', ['name', 'enabled', 'helpUrl', 'description', 'iconClass', 'BonusAward:name', 'BonusAward:numMinutes', 'BonusAward:iconClass', 'startDate', 'endDate']);
propsLookupByItem.set('GlobalBadge', ['name', 'enabled', 'helpUrl', 'description', 'iconClass']);
const excludeLookupFromActions = ['Move', 'ImportFromCatalog', 'ReuseInProject', 'AssignSkill', 'AssignLevel', 'RemoveLevelAssignment', 'RemoveSkillAssignment'];
const valuesMap = new Map();
valuesMap.set('production.mode.enabled', 'Is in the Project Catalog');
valuesMap.set('invite_only', 'Is Private Invite Only Project');
valuesMap.set('project-admins_rank_and_leaderboard_optOut', 'Ranking and Leaderboard Opt-Out');
valuesMap.set('level.points.enabled', 'Use Points For Levels');
valuesMap.set('selfReport.type', 'Self Report Type');
valuesMap.set('help.url.root', 'Root Help URL');
valuesMap.set('group-descriptions', 'Always Show Group Descriptions');
valuesMap.set('show_project_description_everywhere', 'Show Project Description everywhere');
valuesMap.set('quizLength', '# of Questions per Quiz Attempt');
valuesMap.set('quizRandomizeQuestions', 'Randomize Question Order');
valuesMap.set('quizTimeLimit', 'Quiz Time Limit');
valuesMap.set('quizRandomizeAnswers', 'Randomize Answer Order');
valuesMap.set('quizNumberOfAttempts', 'Maximum Number of Attempts');
valuesMap.set('quizPassingReq', 'Passing Requirement');
valuesMap.set('rank_and_leaderboard_optOut', 'Ranking and Leaderboard Opt-Out');
valuesMap.set('home_page', 'Default Home Page');

onMounted(() => {
  loadData();
})

const loadData = () => {
  loading.value = true;
  UserActionsService.getDashboardSingleAction(props.actionId, route.params.projectId, route.params.quizId)
      .then((res) => {
        const propsToShow = propsLookupByItem.get(props.item);
        let loadedObj = res;
        if (!excludeLookupFromActions.includes(props.action) && propsToShow) {
          loadedObj = Object.fromEntries(Object.entries(res)
              .filter(([key]) => propsToShow.includes(key)));
        }
        attributes.value = Object.entries(loadedObj).map((entry) => {
          const replacement = valuesMap.get(entry[1]);
          const value = replacement || entry[1];
          const label = entry[0];
          return {
            label: formatLabel(label),
            value,
            isDescription: label === 'description' || label === 'question' || label === 'userAgreement',
            isTextAreaProp: label === 'transcript' || label === 'captions',
          };
        }).sort((a, b) => a.label.localeCompare(b.label));
      }).finally(() => {
    loading.value = false;
  });
}

const formatLabel = (originalLabel) => {
  return originalLabel
      .replace(/([A-Z])/g, (match) => ` ${match}`)
      .replace(/:/g, (match) => `${match} `)
      .replace(/^./, (match) => match.toUpperCase());
}
</script>

<template>

  <LoadingContainer :is-loading="loading">
    <div v-if="attributes && attributes.length > 0">
      <div v-for="(attr, index) in attributes" :key="attr.label">
        <div v-if="attr.isDescription && attr.value">
          <div class="font-italic">
            {{ attr.label }}:
          </div>
          <Card body-class="py-1" style="height:150px; overflow-y: scroll;">
            <template #content>
              <div>
                <MarkdownText v-if="attr.value"
                              :instance-id="`${attr.label}-${index}`"
                              :text="attr.value"
                              data-cy="descriptionText"/>
              </div>
            </template>
          </Card>
        </div>
        <div v-else class="grid">
          <div class="col-3 font-italic">{{ attr.label }}:</div>
          <div class="col my-auto">
            <span v-if="!attr.value" class="text-secondary">Not Provided</span>
            <span v-else>
              <div v-if="attr.isTextAreaProp">
                <Panel>
                  <p class="m-0">{{ attr.value }}</p>
                </Panel>
              </div>
              <span v-else>{{ attr.value }}</span>
            </span>
          </div>
        </div>
      </div>
    </div>
    <div v-else>
      <i class="fas fa-dragon" aria-hidden="true"/> No additional information to show
    </div>
  </LoadingContainer>
</template>

<style scoped></style>
