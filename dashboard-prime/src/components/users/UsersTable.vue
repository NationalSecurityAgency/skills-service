<script setup>
import { ref, onMounted, computed, nextTick } from 'vue';
import { useRoute } from 'vue-router';
import InputText from 'primevue/inputtext';
import Slider from 'primevue/slider';
import Column from 'primevue/column';
import ProgressBar from 'primevue/progressbar';
import UsersService from './UsersService.js';
import DateCell from '@/components/utils/table/DateCell.vue';
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'

const route = useRoute();
const announcer = useSkillsAnnouncer();

let filters = ref({
  user: '',
  progress: 0,
  minimumPoints: 0,
});

let data = ref([]);
let isLoading = ref(true);
let totalPoints = ref(0);
let currentPage = ref(1);
let totalRows = ref(1);
let pageSize = ref(5);
const possiblePageSizes = [5, 10, 15, 20];
let ascending = ref(false);
let sortBy = ref('lastUpdated');
let showUserTagColumn = ref(true);

onMounted(() => {
  loadData();
});

const applyFilters = () => {
  currentPage.value = 1;
  if (filters.value.progress > 100) {
    filters.value.progress = 100;
  }
  if (filters.value.progress < 0) {
    filters.value.progress = 0;
  }
  filters.value.minimumPoints = Math.floor(totalPoints.value * (filters.value.progress / 100));
  loadData().then(() => {
    let filterMessage = 'Users table has been filtered by';
    if (filters.value.user) {
      filterMessage += ` ${filters.value.user}`;
    }
    if (filters.value.minimumPoints > 0) {
      filterMessage += `${filters.value.user ? ' and' : ''} users with at least ${filters.value.minimumPoints} points`;
    }
    nextTick(() => announcer.polite(filterMessage));
  });
};

const reset = () => {
  filters.value.user = '';
  filters.value.minimumPoints = 0;
  filters.value.progress = 0;
  currentPage.value = 1;
  loadData().then(() => {
    nextTick(() => announcer.polite('Users table filters have been removed'));
  });
};

const getUrl = () => {
  let url = `/admin/projects/${encodeURIComponent(route.params.projectId)}`;
  if (route.params.skillId) {
    url += `/skills/${encodeURIComponent(route.params.skillId)}`;
  } else if (route.params.badgeId) {
    url += `/badges/${encodeURIComponent(route.params.badgeId)}`;
  } else if (route.params.subjectId) {
    url += `/subjects/${encodeURIComponent(route.params.subjectId)}`;
  } else if (route.params.tagKey && route.params.tagFilter) {
    url += `/userTags/${encodeURIComponent(route.params.tagKey)}/${encodeURIComponent(route.params.tagFilter)}`;
  }
  url += '/users';
  return url;
};

const calculateClientDisplayRoute = (props) => {
  const hasSubject = route.params.subjectId || false;
  const hasSkill = route.params.skillId || false;
  const hasBadge = route.params.badgeId || false;

  let routeObj = {
    name: 'ClientDisplayPreview',
    params: {
      projectId: route.params.projectId,
      userId: props.userId,
      dn: props.dn,
    },
  };

  if (hasSkill) {
    routeObj = {
      name: 'ClientDisplayPreviewSkill',
      params: {
        projectId: route.params.projectId,
        subjectId: route.params.subjectId,
        skillId: route.params.skillId,
        userId: props.userId,
        dn: props.dn,
      },
    };
  } else if (hasSubject) {
    routeObj = {
      name: 'ClientDisplayPreviewSubject',
      params: {
        projectId: route.params.projectId,
        subjectId: route.params.subjectId,
        userId: props.userId,
        dn: props.dn,
      },
    };
  } else if (hasBadge) {
    routeObj = {
      name: 'ClientDisplayPreviewBadge',
      params: {
        projectId: route.params.projectId,
        badgeId: route.params.badgeId,
        userId: props.userId,
        dn: props.dn,
      },
    };
  }

  return routeObj;
};

const loadData = () => {
  const url = getUrl();
  isLoading.value = true;
  return UsersService.ajaxCall(url, {
    query: filters.value.user,
    limit: pageSize.value,
    ascending: ascending.value,
    page: currentPage.value,
    byColumn: 0,
    orderBy: sortBy.value,
    minimumPoints: filters.value.minimumPoints,
  }).then((res) => {
    data.value = res.data;
    totalRows.value = res.count;
    totalPoints.value = res.totalPoints;
    isLoading.value = false;
  });
};

const calcPercent = (userPoints) => {
  if (!totalPoints.value) {
    return 'N/A';
  }
  return Math.trunc((userPoints / totalPoints.value) * 100);
};

const pageChanged = (pagingInfo) => {
  pageSize.value = pagingInfo.rows;
  currentPage.value = pagingInfo.page + 1;
  loadData();
};

const sortField = (column) => {
  sortBy.value = column.sortField;
  ascending.value = column.sortOrder === 1;

  // set to the first page
  currentPage.value = 1;
  loadData();
};
</script>

<template>
  <div class="w-full">
    <div class="flex gap-4 my-2">
      <div class="flex flex-column flex-1">
        <label for="userFilter">User Filter</label>
        <InputText id="userFilter" v-model="filters.user" v-on:keydown.enter="applyFilters" data-cy="users-skillIdFilter" aria-label="user filter"/>
      </div>
      <div class="flex flex-column flex-1">
        <div class="flex gap-2">
          <label for="minimumProgress">Minimum User Progress</label>
          <div class="flex flex-1" style="padding-top: 12px;">
            <span>0%</span>
            <div class="flex flex-1 flex-column" style="padding-top: 6px;">
              <Slider v-model="filters.progress" v-on:keydown.enter="applyFilters" :min="0" :max="100"
                       data-cy="users-progress-range" aria-label="user progress range filter"/>
            </div>
            <span style="margin-right:8px;">100%</span>
          </div>
          <div class="flex flex-1">
            <InputText v-model.number="filters.progress" v-on:keydown.enter="applyFilters" :min="0" :max="100"
                       data-cy="users-progress-input" aria-label="user progress input filter" style="width: 80px; margin-right: 6px;"/>
          </div>
        </div>
      </div>
    </div>

    <div class="flex gap-2 my-2">
      <SkillsButton icon="fa fa-filter" label="Filter" @click="applyFilters" data-cy="users-filterBtn" />
      <SkillsButton icon="fa fa-times" label="Reset" @click="reset" class="ml-1" data-cy="users-resetBtn" />
    </div>

    <div>
      <DataTable :value="data" :loading="isLoading" size="small" stripedRows showGridlines paginator lazy
                 :totalRecords="totalRows" :rows="pageSize" @page="pageChanged"
                 tableStoredStateId="usersTable" data-cy="usersTable"
                 :rowsPerPageOptions="possiblePageSizes" @sort="sortField">
        <Column field="userId" header="User" :sortable="true">
          <template #header>
            <i class="fas fa-user skills-color-users" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            {{ slotProps.data.userId }}
            <!--    :aria-label="`View details for user ${getUserDisplay(data.item, true)}`"-->
<!--            <router-link-->
<!--                :to="calculateClientDisplayRoute(slotProps.data)"-->
<!--                role="link">-->
<!--            </router-link>-->
<!--            <SkillsButton :to="calculateClientDisplayRoute(slotProps.data)"-->
<!--                      variant="outline-info" size="sm" class="text-secondary" icon="fa fa-user-alt"-->
<!--                      data-cy="usersTable_viewDetailsBtn"><span class="sr-only">view user details</span>-->
<!--            </SkillsButton>-->
          </template>
        </Column>
        <Column v-if="showUserTagColumn" field="userTag" header="User Tag" ></Column>
        <Column field="totalPoints" header="Progress" :sortable="true">
          <template #header>
            <i class="far fa-arrow-alt-circle-up skills-color-points" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <div :data-cy="`usr_progress-${slotProps.data.userId}`">
              <div class="flex">
                <div class="flex flex-auto">
                  <span class="font-weight-bold text-primary"
                        :aria-label="`${calcPercent(slotProps.data.totalPoints)} percent completed`"
                        data-cy="progressPercent">{{ calcPercent(slotProps.data.totalPoints) }}%</span>
                </div>
                <div class="flex flex-auto justify-content-end">
                  <span class="text-primary font-weight-bold"
                        :aria-label="`${slotProps.data.totalPoints} out of ${totalPoints} total points`"
                        data-cy="progressCurrentPoints">{{ slotProps.data.totalPoints?.toLocaleString() }}</span> / <span class="font-italic" data-cy="progressTotalPoints">{{ totalPoints?.toLocaleString() }}</span>
                </div>
              </div>
              <ProgressBar style="height: 5px;" :value="calcPercent(slotProps.data.totalPoints)" :showValue="false" :aria-label="`Progress for ${slotProps.data.userId} user`" />
              <div v-if="slotProps.data.userMaxLevel || slotProps.data.userMaxLevel === 0" class="row" data-cy="progressLevels">
                <div class="col">
                  <i class="fas fa-trophy skills-color-levels" aria-hidden="true" /> <span class="font-italic">Current Level: </span>
                  <span v-if="slotProps.data.userMaxLevel === 0" data-cy="progressCurrentLevel">None</span>
                  <span v-else class="font-weight-bold" data-cy="progressCurrentLevel">{{ slotProps.data.userMaxLevel }}</span>
                </div>
              </div>
            </div>
          </template>
        </Column>
        <Column field="lastUpdated" header="Points Last Earned" :sortable="true">
          <template #header>
            <i class="far fa-clock skills-color-events" aria-hidden="true"></i>
          </template>
          <template #body="slotProps">
            <date-cell :value="slotProps.data.lastUpdated" />
          </template>
        </Column>

        <template #paginatorstart>
          <span>Total Rows:</span> <span class="font-semibold" data-cy=skillsBTableTotalRows>{{ totalRows }}</span>
        </template>

        <template #empty>
          <div class="flex justify-content-center flex-wrap">
            <i class="flex align-items-center justify-content-center mr-1 fas fa-exclamation-circle"
               aria-hidden="true"></i>
            <span class="flex align-items-center justify-content-center">There are no records to show
              </span>
          </div>
        </template>
      </DataTable>
    </div>
  </div>
</template>

<style scoped></style>
