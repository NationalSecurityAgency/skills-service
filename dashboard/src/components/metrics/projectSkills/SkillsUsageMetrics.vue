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
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import TableNoRes from "@/components/utils/table/TableNoRes.vue";

const route = useRoute();
const numberFormat = useNumberFormat()
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
const isExporting = ref(false)

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

const exportSkills = () => {
  loading.value = true;
  isExporting.value = true;
  MetricsService.exportProjectSkillsMetrics(route.params.projectId)
      .then((dataFromServer) => {
        isExporting.value = false;
        loading.value = false;
      });
}

const totalRows = computed(() => items.value.length);
</script>

<template>
  <Card data-cy="skillsNavigator" :pt="{ body: { class: 'p-0!' } }">
    <template #header>
      <SkillsCardHeader title="Skills"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="p-4">
        <div class="flex gap-4 flex-col xl:flex-row">
          <div class="field flex-1 xl:border-r xl:px-4 xl:pt-4 gap-2">
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
            <div class="flex gap-2 flex-wrap">
              <ToggleButton onLabel="Overlooked Skill" offLabel="Overlooked Skill" v-model="filters.overlookedTag"
                            data-cy="overlookedFilterButton" aria-label="Overlooked Skill" />
              <ToggleButton onLabel="Top Skill" offLabel="Top Skill" v-model="filters.topSkillTag"
                            data-cy="topSkillFilterButton" aria-label="Top Skill" />
              <ToggleButton onLabel="High Activity" offLabel="High Activity" v-model="filters.highActivityTag"
                            data-cy="highActivityFilterButton" aria-label="High Activity" />
              <ToggleButton onLabel="Never Achieved" offLabel="Never Achieved" v-model="filters.neverAchieved"
                            data-cy="neverAchievedFilterButton" aria-label="Never Achieved" />
              <ToggleButton onLabel="Never Reported" offLabel="Never Reported" v-model="filters.neverReported"
                            data-cy="neverReportedFilterButton" aria-label="Never Reported" />
            </div>
            <div class="font-light text-sm mt-1">Please Note: These filters become more meaningful with extensive usage
            </div>
          </div>
        </div>
        <div class="flex flex-1 flex-col gap-2 px-4 pb-4" v-if="tags.length > 0">
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
        <div class="flex xl:pl-4 mb-4 xl:mt-4">
        <SkillsButton variant="outline-info" @click="applyFilters" data-cy="skillsNavigator-filterBtn" icon="fa fa-filter" label="Filter" />
        <SkillsButton variant="outline-info" @click="reset" class="ml-1" data-cy="skillsNavigator-resetBtn" icon="fa fa-times" label="Reset" />
      </div>
      </div>

      <SkillsDataTable :value="items"
                       data-cy="skillsNavigator-table"
                       aria-label="Skill Usage"
                       tableStoredStateId="skillsNavigator-table"
                       paginator
                       show-grid-lines
                       striped-rows
                       :loading="loading"
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes">
        <template #loading>
￼          <div data-cy="skillsNavigator-loading">
￼            <Message v-if="isExporting" icon="fas fa-download" severity="contrast" :closable="false">Exporting, please wait...</Message>
￼            <SkillsSpinner :is-loading="true"></SkillsSpinner>
￼          </div>
        </template>
        <template #header>
          <div class="flex justify-end flex-wrap">
            <SkillsButton :disabled="totalRows <= 0"
                          size="small"
                          icon="fas fa-download"
                          label="Export All Rows"
                          @click="exportSkills"
                          data-cy="exportSkillsTableBtn" />
          </div>
        </template>
        <Column field="skillName" header="Skill" sortable :class="{'flex': isFlex }">
          <template #body="slotProps">
            <div class="flex gap-2 flex-wrap">
              <div class="flex flex-1 flex-col">
                {{ slotProps.data.skillName }}
                <Badge v-if="slotProps.data.isReusedSkill" variant="success" class="text-uppercase"><i class="fas fa-recycle"></i> Reused</Badge>
                <div v-if="slotProps.data.skillTags && slotProps.data.skillTags.length > 0">
                  <Badge v-for="tag in slotProps.data.skillTags" :key="tag.tagId" variant="info" class="mr-2 mt-1">
                    <i :class="'fas fa-tag'" class="ml-1" style="margin-left: 0 !important;" aria-hidden="true"></i> {{ tag.tagValue }}
                  </Badge>
                </div>
              </div>
              <div class="flex gap-1 right-0">
                <router-link target="_blank" :to="{ name: 'SkillOverview', params: { projectId: projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId } }" tabindex="-1">
                  <SkillsButton size="small" class="text-secondary"><i class="fa fa-wrench"/><span class="sr-only">view skill configuration</span></SkillsButton>
                </router-link>
                <router-link :id="`b-skill-metrics_${slotProps.data.skillId}`" target="_blank" :to="{ name: 'SkillMetrics', params: { projectId: projectId, subjectId: slotProps.data.subjectId, skillId: slotProps.data.skillId } }" tabindex="-1">
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
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ numberFormat.pretty(items.length) }}</span>
        </template>

        <template #empty>
          <table-no-res data-cy="emptyTable" />
        </template>
      </SkillsDataTable>
    </template>
  </Card>
</template>

<style scoped>

</style>