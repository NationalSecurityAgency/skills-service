<script setup>
import { ref, onMounted } from 'vue';
import { useRoute } from 'vue-router';
import MetricsService from "@/components/metrics/MetricsService.js";
import SkillsUsageHelper from "@/components/metrics/projectSkills/SkillsUsageHelper.js";
import DateCell from "@/components/utils/table/DateCell.vue";
import InputText from "primevue/inputtext";

const route = useRoute();

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
const totalRows = ref(1);

const tableOptions = ref({
  sortBy: 'timestamp',
  sortDesc: true,
  bordered: true,
  outlined: true,
  rowDetailsControls: false,
  stacked: 'md',
  tableDescription: 'Skill Metrics',
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
        totalRows.value = items.value.length;
        loading.value = false;
      });
};
</script>

<template>
  <Card data-cy="skillsNavigator">
    <template #header>
      <SkillsCardHeader title="Skills"></SkillsCardHeader>
    </template>
    <template #content>
      <div class="flex gap-3">
        <div class="flex flex-1 flex-column border-right-1 px-3 pt-3 gap-2">
          <label for="skillFilter">Skill Name Filter</label>
          <InputText class="w-full"
                     v-model="filters.name"
                     id="skillFilter"
                     data-cy="skillsNavigator-skillNameFilter"
                     @keydown.enter="applyFilters"
                     aria-label="Skill name filter" />
        </div>
        <div class="flex flex-1 flex-column gap-2" data-cy="skillsNavigator-filters">
          <label>Skill Usage Filters</label>
          <div class="flex gap-2">
            <ToggleButton onLabel="Overlooked Skill" offLabel="Overlooked Skill" v-model="filters.overlookedTag" />
            <ToggleButton onLabel="Top Skill" offLabel="Top Skill" v-model="filters.topSkillTag" />
            <ToggleButton onLabel="High Activity" offLabel="High Activity" v-model="filters.highActivityTag" />
            <ToggleButton onLabel="Never Achieved" offLabel="Never Achieved" v-model="filters.neverAchieved" />
            <ToggleButton onLabel="Never Reported" offLabel="Never Reported" v-model="filters.neverReported" />
          </div>
            <div class="font-light text-sm">Please Note: These filters become more meaningful with extensive usage</div>
        </div>
      </div>
      <div class="flex flex-1 flex-column gap-2 px-3" v-if="tags.length > 0">
        <label>Skill Tags</label>
        <div class="flex gap-2">
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
      <div class="flex pl-3 mb-3 mt-3">
        <SkillsButton variant="outline-info" @click="applyFilters" data-cy="skillsNavigator-filterBtn" icon="fa fa-filter" label="Filter" />
        <SkillsButton variant="outline-info" @click="reset" class="ml-1" data-cy="skillsNavigator-resetBtn" icon="fa fa-times" label="Reset" />
      </div>

      <SkillsDataTable :value="items"
                       data-cy="skillsNavigator-table"
                       tableStoredStateId="skillsNavigator-table"
                       paginator
                       show-grid-lines
                       striped-rows
                       :totalRecords="totalRows"
                       :rows="pageSize"
                       :rowsPerPageOptions="possiblePageSizes">
        <Column field="skillName" header="Skill" sortable>
          <template #body="slotProps">
            <div class="flex">
              <div class="flex flex-1 flex-column">
                {{ slotProps.data.skillName }} <Badge v-if="slotProps.data.isReusedSkill" variant="success" class="text-uppercase"><i class="fas fa-recycle"></i> Reused</Badge>
                <div v-if="slotProps.data.skillTags.length > 0">
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
        <Column field="numUserAchieved" header="# Users Achieved" sortable>
          <template #body="slotProps">
            <span class="ml-2">{{ slotProps.data.numUserAchieved }}</span>
            <Badge v-if="slotProps.data.isOverlookedTag" variant="danger" class="ml-2">Overlooked Skill</Badge>
            <Badge v-if="slotProps.data.isTopSkillTag" variant="info" class="ml-2">Top Skill</Badge>
          </template>
        </Column>
        <Column field="numUsersInProgress" header="# Users In Progress" sortable>
          <template #body="slotProps">
            <span class="ml-2">{{ slotProps.data.numUsersInProgress }}</span>
            <Badge v-if="slotProps.data.isHighActivityTag" variant="success" class="ml-2">High Activity</Badge>
          </template>
        </Column>
        <Column field="lastAchievedTimestamp" header="Last Achieved" sortable>
          <template #body="slotProps">
            <Badge v-if="slotProps.data.isNeverAchievedTag" variant="warning" class="ml-2">Never</Badge>
            <div v-else>
              <date-cell :value="slotProps.data.lastAchievedTimestamp" />
            </div>
          </template>
        </Column>
        <Column field="lastReportedTimestamp" header="Last Reported" sortable>
          <template #body="slotProps">
            <Badge v-if="slotProps.data.isNeverReportedTag" variant="warning" class="ml-2">Never</Badge>
            <div v-else>
              <date-cell :value="slotProps.data.lastReportedTimestamp" />
            </div>
          </template>
        </Column>
      </SkillsDataTable>
    </template>
  </Card>
</template>

<style scoped>

</style>