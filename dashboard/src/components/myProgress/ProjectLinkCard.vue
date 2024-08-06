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
import { computed, ref } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import CardWithVericalSections from '@/components/utils/cards/CardWithVericalSections.vue'
import { useMyProgressState } from '@/stores/UseMyProgressState.js'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js';
import {useDialogMessages} from "@/components/utils/modal/UseDialogMessages.js";

const props = defineProps(['proj', 'displayOrder'])
const emit = defineEmits(['sort-changed-requested', 'remove-project'])
const numberFormat = useNumberFormat()
const myProgressState = useMyProgressState()
const themeHelper = useThemesHelper()
const dialogMessages = useDialogMessages()

const showSortControl = computed(() => myProgressState.myProjects.length > 1)

const currentProgressPercent = props.proj.totalPoints ?
  Math.trunc((props.proj.points / props.proj.totalPoints) * 100) : 0

const overSortControl = ref(false)
const series = [currentProgressPercent]
const chartOptions = {
  chart: {
    height: 200,
    type: 'radialBar',
    offsetY: -15,
    offsetX: -15
  },
  plotOptions: {
    radialBar: {
      startAngle: -135,
      endAngle: 135,
      hollow: {
        size: '70%',
        margin: 30,
      },
      dataLabels: {
        show: true,
        name: {
          show: false
        },
        value: {
          offsetY: 10,
          fontSize: '22px',
          color: themeHelper.isDarkTheme ? 'white' : '#303030',
          formatter(val) {
            return `${val}%`
          }
        }
      }
    }
  },
  fill: {
    colors: ['#de0f0f'],
    type: 'solid',
    gradient: {
      shade: 'dark',
      shadeIntensity: 0.15,
      inverseColors: false,
      opacityFrom: 1,
      opacityTo: 1,
      stops: [0, 50, 65, 91]
    }
  },
  stroke: {
    dashArray: 4
  },
  labels: ['Median Ratio']
}
const rankVariant = ref('info')

const moveDown = () => {
  emit('sort-changed-requested', {
    projectId: props.proj.projectId,
    direction: 'down'
  })
}
const moveUp = () => {
  emit('sort-changed-requested', {
    projectId: props.proj.projectId,
    direction: 'up'
  })
}
const remove = () => {
  dialogMessages.msgConfirm({
    message: 'Remove this project from My Projects?',
    header: 'Please Confirm!',
    acceptLabel: 'YES, Remove It',
    accept: () => {
      emit('remove-project', props.proj.projectId);
    }
  });
}

</script>

<template>
  <CardWithVericalSections
        :data-cy="`project-link-card-${proj.projectId}`"
        :class="{ 'proj-link-card' : !overSortControl }">
    <template #header>
      <div class="flex">
        <div class="flex-1">
          <SkillsButton
            v-if="showSortControl"
            :id="`sortControlHandle-${proj.projectId}`"
            text
            icon="fas fa-arrows-alt"
            severity="secondary"
            class="pl-2 pr-3 sort-control border-top-none border-left-none border-right-1 border-bottom-1 surface-border text-color-secondary"
            :aria-label="`Sort Control. Current position for ${proj.projectName} is ${displayOrder}. Press up or down to change the order.`"
            size="large"
            data-cy="sortControlHandle"
            @keyup.down="moveDown"
            @keyup.up="moveUp"
          ></SkillsButton>
        </div>
        <div class="text-right">
          <!--            todo: support of project removal -->
          <SkillsButton
            text
            icon="far fa-times-circle"
            severity="secondary"
            class="pr-3 pt-2"
            :data-cy="`remove-${proj.projectId}Btn`"
            @click="remove"
            :aria-label="`Remove ${proj.projectName} from My Projects`"
            size="large"></SkillsButton>
        </div>
      </div>
    </template>
    <template #content>
      <div :class="{'pt-4': !showSortControl }">

        <div class="flex flex-column sm:flex-row align-items-center">
          <div class="pt-3" style="min-width: 200px;">
            <apexchart type="radialBar" height="200" width="200" :options="chartOptions"
                       :series="series"></apexchart>
          </div>
          <div class="flex-1 pt-0 pr-3 text-center sm:text-right">
            <div class="uppercase text-2xl text-primary skills-break-word" data-cy="project-card-project-name"
                 :aria-label="`Project ${proj.projectName}`" :title="proj.projectName ">{{ proj.projectName }}
            </div>
            <div class="text-2xl text-color-secondary mt-2" data-cy="project-card-project-level">
              {{ proj.levelDisplayName }} {{ proj.level }}
            </div>
            <div data-cy="project-card-project-rank" class="mt-1">
              <Tag :severity="rankVariant" :aria-label="`Ranked ${proj.rank} out of ${proj.totalUsers} project users`">
                Rank: {{ numberFormat.pretty(proj.rank) }} / {{ numberFormat.pretty(proj.totalUsers) }}
              </Tag>
            </div>
          </div>
        </div>



      </div>
    </template>
    <template #footer>
      <div class="text-right mx-3">
        <div :id="`projectProgressLabel_${proj.projectId}`"
             class="small mb-1"
             :aria-label="`${proj.points} out of ${proj.totalPoints} available points`"
             data-cy="project-card-project-points">
          <span class="text-color-warn text-xl">{{ numberFormat.pretty(proj.points) }}</span>
          <span>/</span>
          <span>{{ numberFormat.pretty(proj.totalPoints) }}</span>
        </div>
        <ProgressBar
          :value="currentProgressPercent"
          :aria-label="`${currentProgressPercent} percent complete`"
          :show-value="false"
          style="height: 6px" />
      </div>

      <div class="px-3 w-full text-center pt-4 pb-3">
        <router-link tabindex="-1"
                     :to="{ path: `/progress-and-rankings/projects/${proj.projectId}` }"
                     :data-cy="`project-link-${proj.projectId}`">
          <Button
            label="View"
            icon="far fa-eye"
            :aria-label="`Click to navigate to ${proj.projectName} project page.`"
            outlined class="w-full" size="small"/>
        </router-link>
      </div>
    </template>
  </CardWithVericalSections>
</template>

<style scoped>

</style>