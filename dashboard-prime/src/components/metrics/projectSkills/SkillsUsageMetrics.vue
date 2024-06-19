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
import { ref, onMounted, computed } from 'vue'
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import SkillsUsageHelper from "@/components/metrics/projectSkills/SkillsUsageHelper.js";
import DateCell from "@/components/utils/table/DateCell.vue";
import InputText from "primevue/inputtext";
import NumberFormatter from '@/components/utils/NumberFormatter.js'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import Column from 'primevue/column'

const route = useRoute();
const responsive = useResponsiveBreakpoints()
const isFlex = computed(() => responsive.lg.value)

onMounted(() => {
  loadData();
});

const projectId = ref(route.params.projectId);
const filters = ref({
  name: '',
  highActivityTag: false,
  overlookedTag: false,
  topSkillTag: false,
  neverAchieved: false,
  neverReported: false,
  skillTags: [],
});
const loading = ref(false);

const pageSize = 5;
const possiblePageSizes = [5, 10, 15, 20, 50];

const tableOptions = ref({
  sortBy: 'timestamp',
  sortDesc: true,
});
const items = ref([]);
const originalItems = ref([]);
const tags = ref([]);


const applyFilters = () => {
  items.value = originalItems.value.filter((item) => SkillsUsageHelper.shouldKeep(filters.value, item));
};

const reset = () => {
  filters.value.name = '';
  filters.value.neverAchieved = false;
  filters.value.neverReported = false;
  filters.value.overlookedTag = false;
  filters.value.topSkillTag = false;
  filters.value.highActivityTag = false;
  filters.value.skillTags = [];
  items.value = originalItems.value;
};

const loadData = () => {
  loading.value = true;
  MetricsService.loadChart(route.params.projectId, 'skillUsageNavigatorChartBuilder')
      .then((dataFromServer) => {
        items.value = SkillsUsageHelper.addTags(dataFromServer.skills);
        tags.value = dataFromServer.tags;
        originalItems.value = items.value;
        loading.value = false;
      });
};
</script>

<template>
  <Card data-cy="skillsNavigator"  :pt="{ body: { class: 'p-0' }, content: { class: 'p-0' } }">
    <template #header>
      <SkillsCardHeader title="Skills"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="p-3">
        <div class="flex gap-3 flex-column xl:flex-row">
          <div class="field flex-1 xl:border-right-1 xl:px-3 xl:pt-3 gap-2">
            <label for="skillFilter">Skill Name Filter</label>
            <InputText class="w-full"
                       v-model="filters.name"
                       id="skillFilter"
                       data-cy="skillsNavigator-skillNameFilter"
                       @keydown.enter="applyFilters"
                       aria-label="Skill name filter" />
          </div>
          <div class="field flex-1 " data-cy="skillsNavigator-filters">
            <label>Skill Usage Filters</label>
            <div class="flex gap-2">
              <ToggleButton onLabel="Overlooked Skill" offLabel="Overlooked Skill" v-model="filters.overlookedTag"
                            data-cy="overlookedFilterButton" />
              <ToggleButton onLabel="Top Skill" offLabel="Top Skill" v-model="filters.topSkillTag"
                            data-cy="topSkillFilterButton" />
              <ToggleButton onLabel="High Activity" offLabel="High Activity" v-model="filters.highActivityTag"
                            data-cy="highActivityFilterButton" />
              <ToggleButton onLabel="Never Achieved" offLabel="Never Achieved" v-model="filters.neverAchieved"
                            data-cy="neverAchievedFilterButton" />
              <ToggleButton onLabel="Never Reported" offLabel="Never Reported" v-model="filters.neverReported"
                            data-cy="neverReportedFilterButton" />
            </div>
            <div class="font-light text-sm mt-1">Please Note: These filters become more meaningful with extensive usage
            </div>
          </div>
        </div>
        <div class="flex flex-1 flex-column gap-2 px-3" v-if="tags.length > 0">
          <label>Skill Tags</label>
          <div class="flex gap-2" data-cy="skillTag-filters">
            <div v-for="tag in tags" :key="tag.tagId">
              <Checkbox v-model="filters.skillTags" :value="tag.tagId" :name="tag.tagId" :inputId="tag.tagId"></Checkbox>
              <label :for="tag.tagId">
                <Badge severity="info" class="ml-2">
                  <i :class="'fas fa-tag'" class="ml-1" style="margin-left: 0 !important;" aria-hidden="true"></i> {{tag.tagValue}}
                </Badge>
              </label>
            </div>
          </div>
        </div>
        <div class="flex xl:pl-3 mb-3 xl:mt-3">
        <SkillsButton variant="outline-info" @click="applyFilters" data-cy="skillsNavigator-filterBtn" icon="fa fa-filter" label="Filter" />
        <SkillsButton variant="outline-info" @click="reset" class="ml-1" data-cy="skillsNavigator-resetBtn" icon="fa fa-times" label="Reset" />
      </div>
      </div>

      <SkillsDataTable :value="items"
                       data-cy="skillsNavigator-table"
                       tableStoredStateId="skillsNavigator-table"
                       paginator
                       show-grid-lines
                       striped-rows
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes">
        <Column field="skillName" header="Skill" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <div class="flex gap-2 flex-wrap">
              <div class="flex flex-1 flex-column">
                {{ slotProps.data.skillName }}
                <Badge v-if="slotProps.data.isReusedSkill" variant="success" class="text-uppercase"><i class="fas fa-recycle"></i> Reused</Badge>
                <div v-if="slotProps.data.skillTags && slotProps.data.skillTags.length > 0">
                  <Badge v-for="tag in slotProps.data.skillTags" :key="tag.tagId" variant="info" class="mr-2 mt-1">
                    <i :class="'fas fa-tag'" class="ml-1" style="margin-left: 0 !important;" aria-hidden="true"></i> {{ tag.tagValue }}
                  </Badge>
                </div>
              </div>
              <div class="flex gap-1 right-0">
                <router-link target="_blank" :to="{ name: 'SkillOverview', params: { projectId: projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId } }">
                  <SkillsButton size="small" class="text-secondary"><i class="fa fa-wrench"/><span class="sr-only">view skill configuration</span></SkillsButton>
                </router-link>
                <router-link :id="`b-skill-metrics_${slotProps.data.skillId}`" target="_blank" :to="{ name: 'SkillMetrics', params: { projectId: projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId } }">
                  <SkillsButton variant="outline-info" size="small" class="text-secondary"><i class="fa fa-chart-bar"/><span class="sr-only">view skill metrics</span></SkillsButton>
                </router-link>
              </div>
            </div>
          </template>
        </Column>
        <Column field="numUserAchieved" header="# Users Achieved" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <span class="ml-2">{{ NumberFormatter.format(slotProps.data.numUserAchieved) }}</span>
            <Badge v-if="slotProps.data.isOverlookedTag" variant="danger" class="ml-2">Overlooked Skill</Badge>
            <Badge v-if="slotProps.data.isTopSkillTag" variant="info" class="ml-2">Top Skill</Badge>
          </template>
        </Column>
        <Column field="numUsersInProgress" header="# Users In Progress" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <span class="ml-2">{{ NumberFormatter.format(slotProps.data.numUsersInProgress) }}</span>
            <Badge v-if="slotProps.data.isHighActivityTag" variant="success" class="ml-2">High Activity</Badge>
          </template>
        </Column>
        <Column field="lastReportedTimestamp" header="Last Reported" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <Badge v-if="slotProps.data.isNeverReportedTag" variant="warning" class="ml-2">Never</Badge>
            <div v-else>
              <date-cell :value="slotProps.data.lastReportedTimestamp" />
            </div>
          </template>
        </Column>
        <Column field="lastAchievedTimestamp" header="Last Achieved" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <Badge v-if="slotProps.data.isNeverAchievedTag" variant="warning" class="ml-2">Never</Badge>
            <div v-else>
              <date-cell :value="slotProps.data.lastAchievedTimestamp" />
            </div>
          </template>
        </Column>

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ items.length }}</span>
        </template>

        <template #empty>
          <div class="flex justify-content-center flex-wrap" data-cy="emptyTable">
            <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle" aria-hidden="true"></i>
            <span class="flex align-items-center justify-content-center">There are no records to show</span>
          </div>
        </template>
      </SkillsDataTable>
    </template>
  </Card>
</template>

<style scoped>

</style>