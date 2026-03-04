/*
Copyright 2026 SkillTree

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
import {computed, onMounted, ref} from "vue";
import UsersService from "@/components/users/UsersService.js";
import DataView from 'primevue/dataview';
import {useColors} from "@/skills-display/components/utilities/UseColors.js";
import DateCell from "@/components/utils/table/DateCell.vue";
import ProgressBar from "primevue/progressbar";
import QuizRunsTable from "@/components/quiz/runsHistory/QuizRunsTable.vue";


const props = defineProps({
  userProgressMeta: Object,
})
const colors = useColors()

const isLoading = ref(true)
const singleUserProjectProgress = ref({})

onMounted(() => {
  loadData()
})

const loadData = () => {
  return UsersService.getGlobalSingleUserProgress(props.userProgressMeta.userId).then((data) => {
    singleUserProjectProgress.value = data.projectsProgress.map((item) => {
      return {
        ... item,
        pointsProgressPercent: item.projectTotalPoints > 0 ? Math.round((item.points/item.projectTotalPoints)*100) : 0
      }
    })
  }).finally(() => {
    isLoading.value = false
  })
}

const title = computed(() => {
  const userId = props.userProgressMeta.userToShow
  return `${userId} Overall Progress`
})
</script>

<template>
  <div class="py-5 pl-5">
    <div v-if="isLoading" class="flex">
      <skills-spinner :is-loading="isLoading"/>
    </div>

    <div v-if="!isLoading">
      <Card>
        <template #header>
          <SkillsCardHeader :title="title"></SkillsCardHeader>
        </template>
        <template #content>
          <div class="pt-4 pb-1 font-bold uppercase border-b-2">
            Projects Progress
          </div>

          <DataView :value="singleUserProjectProgress" class="mb-8">
            <template #empty>This user hasn't made progress in any projects yet</template>
            <template #list="slotProps">
              <div class="flex flex-col">
                <div v-for="(item, index) in slotProps.items" :key="index">
                  <div class="flex gap-4 border-b py-7 px-2 bg-surface-50">
                    <div class="flex-1 flex flex-col">
                      <div class="flex gap-2 mb-2">
                        <div class="text-xl font-bold text-primary flex-1"><i class="fa-solid fa-tasks" aria-hidden="true" /> {{ item.projectName }}</div>
                        <router-link :to="`/administrator/projects/${item.projectId}/users/${userProgressMeta.userId}`" tabindex="-1">
                          <SkillsButton label="View in Project" size="small"/>
                        </router-link>
                      </div>

                      <div class="flex flex-col gap-1">
                        <div class="flex gap-3">
                          <div class="text-primary flex-1"
                               :aria-label="`${item.pointsProgressPercent} percent completed`"
                               data-cy="progressPercent">{{ item.pointsProgressPercent }}%
                          </div>

                          <div
                              :aria-label="`${item.points} out of ${item.projectTotalPoints} total points`"
                          ><span class="text-primary">{{ item.points }}</span> / {{ item.projectTotalPoints }} Points</div>
                        </div>
                        <div>
                          <ProgressBar style="height: 5px;" :value="item.pointsProgressPercent" :showValue="false"
                                       class="lg:min-w-[12rem] xl:min-w-[20rem]"
                                       :aria-label="`Points for ${userProgressMeta.userToShow} user`"/>
                        </div>
                        <div class="flex gap-1 mt-1">Last Progress: <date-cell :value="item.updated" :exclude-time-from-now="true"/></div>
                      </div>
                    </div>
                    <Card>
                      <template #content>
                        <div class="flex">
                          <div class="flex flex-col gap-2 items-center">
                            <div>
                              Level
                              <Tag>{{ item.achievedProjLevel }}</Tag>
                              out of
                              <Tag severity="secondary">{{ item.numProjectLevels }}</Tag>
                            </div>
                            <Rating
                                v-model="item.achievedProjLevel"
                                :stars="item.numProjectLevels"
                                readonly
                                disabled
                                :cancel="false"
                                aria-hidden="true"
                                class="override-readonly-opacity"/>
                          </div>
                        </div>
                      </template>
                    </Card>
                    <Card>
                      <template #content>
                        <div class="flex flex-col gap-2 items-center">
                          <div><Tag>{{ item.numAchievedSkills }}</Tag> out of <Tag severity="secondary">{{ item.numSkills }}</Tag></div>
                          <div class="uppercase">Skills</div>
                        </div>
                      </template>
                    </Card>
                    <Card>
                      <template #content>
                        <div class="flex flex-col gap-2 items-center">
                          <div><Tag>{{ item.numAchievedBadges }}</Tag> out of <Tag severity="secondary">{{ item.numBadges }}</Tag></div>
                          <div class="uppercase">Badges</div>
                        </div>
                      </template>
                    </Card>
                  </div>
                </div>
              </div>
            </template>
          </DataView>


          <div class="pt-4 pb-1 font-bold uppercase border-b-2">
            Quiz and Survey Runs
          </div>

          <quiz-runs-table
              table-stored-state-id="singleUserOverviewQuizRunsHistoryTable"
              :only-runs-for-user-id="userProgressMeta.userId"
              :show-controls="false"
              :show-quiz-name-and-type-columns="true"
              :enable-to-show-user-tag-column="false">
            <template #noResults>
              This user hasn't completed any quizzes or surveys yet
            </template>
          </quiz-runs-table>
        </template>
      </Card>
    </div>
  </div>

</template>

<style scoped>

</style>