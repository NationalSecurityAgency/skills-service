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
import { onMounted, ref } from 'vue'
import { useSkillsDisplayService } from '@/skills-display/services/UseSkillsDisplayService.js'
import { useSkillsAnnouncer } from '@/common-components/utilities/UseSkillsAnnouncer.js'
import { useRoute } from 'vue-router'
import SkillsDataTable from '@/components/utils/table/SkillsDataTable.vue'
import Column from 'primevue/column'
import { useResponsiveBreakpoints } from '@/components/utils/misc/UseResponsiveBreakpoints.js'
import { useAppConfig } from '@/common-components/stores/UseAppConfig.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import VerticalProgressBar from '@/skills-display/components/progress/VerticalProgressBar.vue'
import DateCell from '@/components/utils/table/DateCell.vue'
import NoContent2 from '@/components/utils/NoContent2.vue'

const skillsDisplayService = useSkillsDisplayService()
const route = useRoute()
const announcer = useSkillsAnnouncer()
const responsive = useResponsiveBreakpoints()
const appConfig = useAppConfig()
const colors = useColors()
const numFormat = useNumberFormat()

const options = ref([{
  value: 'topTen',
  label: 'Top 10'
}, {
  value: 'tenAroundMe',
  label: '10 Around Me'
}])
const selected = ref(options.value[0])
const onOptionChange = (newVal) => {
  loadData(newVal.value)
}

const loading = ref(true)
const availablePoints = ref(0)
const optedOut = ref(false)
const items = ref([])

onMounted(() => {
  loadData(selected.value.value)
})

const loadData = (type) => {
  loading.value = true
  skillsDisplayService.getLeaderboard(route.params.subjectId, type)
    .then((result) => {
      availablePoints.value = result.availablePoints
      items.value = result.rankedUsers
      optedOut.value = result.optedOut
    })
    .finally(() => {
      loading.value = false
      announcer.polite('Leaderboard data loaded')
    })
}

const isEmpty = (s) => {
  return (!s || typeof s !== 'string' || !s.trim())
}
const getUser = (item) => {
  if (appConfig.isPkiAuthenticated) {
    return isEmpty(item.nickname)
      ? `${item.firstName} ${item.lastName} (${item.userId})`
      : `${item.nickname} (${item.userId})`
  }
  return isEmpty(item.nickname)
    ? item.userId
    : `${item.nickname} (${item.userId})`
}

const getProgressPercent = (item) => {
  if (item.points > 0 && availablePoints.value > 0) {
    return Math.trunc((item.points / availablePoints.value) * 100)
  }
  return 0
}


</script>

<template>
  <Card :pt="{ body: { class: 'p-0 m-0' }}" data-cy="leaderboard">
    <template #header>
      <div class="flex-column sm:flex-row flex gap-1 pt-3 px-3">
        <div class="uppercase text-2xl flex-1">Leaderboard</div>
        <div v-if="!optedOut">
          <SelectButton v-model="selected"
                        :options="options"
                        @update:modelValue="onOptionChange"
                        data-cy="badge-selector"
                        aria-label="Select Top 10 or 10 Around Me">
            <template #option="slotProps">
              <span :data-cy="`select-${slotProps.option.value}`">{{ slotProps.option.label }}</span>
            </template>
          </SelectButton>
        </div>
      </div>
    </template>

    <template #content>
      <Message v-if="optedOut" class="mx-3" :closable="false" severity="warn" icon="fas fa-users-slash">
        You selected to <b>opt-out</b> from ranking. Your name will <b>not</b> appear in the leaderboard and you will
        not be ranked against other users.
        If you want to re-enter the leaderboard and ranking then please adjust your preferences in the SkillTree
        dashboard.
      </Message>

      <div v-if="!optedOut">
        <SkillsDataTable
          :loading="loading"
          tableStoredStateId="skillsDisplayLeaderboard"
          :value="items"
          aria-label="Leaderboard"
          data-cy="leaderboardTable">
          <template #empty>
            <no-content2
              class="my-5"
              title="No Users" message="Leaderboard is empty because there no users with points yet..." />
          </template>

          <Column field="rank" header="Rank" :sortable="false" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-sort-amount-up mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <Tag :aria-label="`Ranked number ${slotProps.data.rank}`">#{{ slotProps.data.rank }}</Tag>
            </template>
          </Column>
          <Column field="userId" header="User" :sortable="false" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="far fa-user mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <div class="text-left flex align-items-center" data-cy="userColumn">
                <Avatar icon="fas fa-user skills-theme-primary-color" class="mr-2" shape="circle" />
                <div class="align-text-bottom text-info skills-theme-primary-color">{{ getUser(slotProps.data) }}</div>
                <i v-if="slotProps.data.rank <=3" class="fas fa-medal ml-2"
                   :class="colors.getRankTextClass(slotProps.data.rank)"
                   aria-hidden="true"></i>
                <div v-if="slotProps.data.isItMe"
                     aria-label="this is you"
                     class="ml-2">
                  <Tag><i class="far fa-hand-point-left mr-1" aria-hidden="true"></i> You!</Tag>
                </div>
              </div>
            </template>
          </Column>
          <Column field="points" header="Progress" :sortable="false" :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="fas fa-running mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <div>
                <span class="font-medium">{{ numFormat.pretty(slotProps.data.points) }}</span> <span
                class="font-italic">Points</span>
              </div>
              <vertical-progress-bar
                :total-progress="getProgressPercent(slotProps.data)"
                :bar-size="5"
              />
            </template>
          </Column>
          <Column field="userFirstSeenTimestamp" header="User Since" :sortable="false"
                  :class="{'flex': responsive.md.value }">
            <template #header>
              <i class="far fa-clock mr-1" aria-hidden="true"></i>
            </template>
            <template #body="slotProps">
              <date-cell :value="slotProps.data.userFirstSeenTimestamp" :exclude-time="true" />
            </template>
          </Column>


        </SkillsDataTable>

      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>