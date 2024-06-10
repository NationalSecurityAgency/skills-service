<script setup>
import { computed, ref } from 'vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'

const props = defineProps(['proj', 'displayOrder'])
const emit = defineEmits(['sort-changed-requested'])
const numberFormat = useNumberFormat()

const overSortControl = ref(false)
const series = [0]
const chartOptions = {
  chart: {
    height: 200,
    type: 'radialBar',
    offsetY: -15,
    offsetX: 0
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
          color: undefined,
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

const currentProgressPercent = computed(() => Math.trunc(props.proj.points / props.proj.totalPoints))
</script>

<template>
  <Card :pt="{ content: { class: 'p-0' }, body: { class: 'p-0 pb-2' } }"
        :data-cy="`project-link-card-${proj.projectId}`"
        class="conic"
        :class="{ 'proj-link-card' : !overSortControl }">
    <template #content>
      <div>
        <div class="flex">
          <div class="flex-1">
            <SkillsButton
              text
              icon="fas fa-arrows-alt"
              severity="secondary"
              class="pl-2 pr-3 surface-ground sort-control"
              :aria-label="`Sort Control. Current position for ${proj.projectName} is ${displayOrder}. Press up or down to change the order.`"
              size="large"
              @keyup.down="moveDown"
              @keyup.up="moveUp"
            ></SkillsButton>
          </div>
          <div class="text-right">
            <SkillsButton
              text
              icon="far fa-times-circle"
              severity="secondary"
              class="pr-3 pt-2"
              :aria-label="`Remove ${proj.projectName} from My Projects`"
              size="large"></SkillsButton>
          </div>

        </div>
        <div class="flex">
          <div class="flex-grow-0 pt-3" style="width: 200px;">
            <apexchart type="radialBar" height="200" :options="chartOptions"
                       :series="series"></apexchart>
          </div>
          <div class="flex-grow-1 pt-0 pr-3 text-right">
            <div class="uppercase text-2xl text-primary" data-cy="project-card-project-name"
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

        <div class="text-right mx-3" style="marginTop: -30px">
          <div :id="`projectProgressLabel_${proj.projectId}`"
               class="small mb-1"
               :aria-label="`${proj.points} out of ${proj.totalPoints} available points`"
               data-cy="project-card-project-points">
            <span class="text-xl text-orange-700">{{ numberFormat.pretty(proj.points) }}</span>
            <span>/</span>
            <span>{{ numberFormat.pretty(proj.totalPoints) }}</span>
          </div>
          <ProgressBar
            :value="currentProgressPercent"
            :aria-label="`${currentProgressPercent} percent complete`"
            style="height: 6px" />
        </div>

        <div class="px-3 w-full text-center mt-4">
          <router-link
            :to="{ path: `/progress-and-rankings/projects/${proj.projectId}` }"
            :aria-label="`Click to navigate to ${proj.projectName} project page.`"
            :data-cy="`project-link-${proj.projectId}`">
            <Button
              label="View"
              icon="far fa-eye"
              outlined class="w-full" size="small"/>
          </router-link>
        </div>


      </div>
    </template>
  </Card>
</template>

<style scoped>

</style>