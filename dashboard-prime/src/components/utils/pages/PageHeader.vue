<script setup>
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'

const props = defineProps(['loading', 'options'])
const numberFormat = useNumberFormat()
const colors = useColors()
</script>

<template>
  <div data-cy="pageHeader" class="border-1 border-round-md surface-border font-medium surface-0 mt-2 px-3 py-3">
    <skills-spinner v-if="loading" :is-loading="loading" />
    <div v-if="!loading" class="flex py-1 px-1 w-full">
      <slot name="banner"></slot>
      <div class="flex w-full flex-wrap">
        <div class="mt-2 text-center lg:text-left w-full lg:w-auto">
          <div class="text-2xl flex">
            <Avatar v-if="options.icon" class="mr-2" :icon="options.icon" />
            <div class="text-2xl" data-cy="title">{{ options.title }}</div>
            <slot name="right-of-header"></slot>
          </div>
          <div v-if="options.subTitle" data-cy="subTitle" class="mt-1 mb-2">
            <slot name="subTitle">
              <div class="text-xl">{{ options.subTitle }}</div>
            </slot>
          </div>
          <slot name="subSubTitle" data-cy="subSubTitle"></slot>
        </div>
        <div class="flex-1">
          <div class="flex justify-content-center lg:justify-content-end flex-wrap">
            <div v-for="(stat, index) in options.stats" :key="stat.label" data-cy="pageHeaderStat" class="w-full md:w-6 lg:w-auto mb-1">
              <Card class="ml-3 mt-2 h-full" :pt="{ body: { class: 'p-3' }, content: { class: 'p-0' } }">
                <template #content>
                  <div class="flex">
                    <div class="" :data-cy="`pageHeaderStat_${stat.label}`">
                      <div class="uppercase text-color-secondary">{{ stat.label }}</div>
                      <div class="font-bold text-xl">
                        <span v-if="stat.preformatted" data-cy="statPreformatted" v-html="stat.preformatted" />
                        <span v-else data-cy="statValue">{{ numberFormat.pretty(stat.count) }}</span>
                      </div>

                      <span v-if="stat.warnMsg" class="ml-1">
                            <i class="fa fa-exclamation-circle text-warning"
                               :aria-label="`Warning: ${stat.warnMsg}`"
                               role="alert"
                               v-tooltip="stat.warnMsg" />
                          </span>
                    </div>
                    <div class="ml-3 flex-1 text-right">
                      <i :class="`${stat.icon} ${colors.getTextClass(index)}`" style="font-size: 2.2rem;" aria-hidden="true"></i>
                    </div>
                  </div>
                  <div class="text-left" style="font-size:0.9rem;" v-if="stat.secondaryPreformatted"
                       v-html="stat.secondaryPreformatted"
                       :data-cy="`pageHeaderStatSecondaryLabel_${stat.label}`"></div>
                  <div v-if="stat.secondaryStats" class="mt-2">
                    <div v-for="secCount in stat.secondaryStats" :key="secCount.label">
                      <div v-if="secCount.count > 0" class="text-left">
                        <Tag
                          :severity="`${secCount.badgeVariant}`"
                               :data-cy="`pageHeaderStats_${stat.label}_${secCount.label}`">
                          {{ numberFormat.pretty(secCount.count) }}
                        </Tag>
                        <span class="text-left uppercase ml-1"
                              style="font-size: 0.8rem">{{ secCount.label }}</span>
                      </div>
                    </div>
                  </div>
                </template>
              </Card>
            </div>
          </div>
        </div>
      </div>
      <slot name="footer"/>
    </div>
  </div>
</template>

<style scoped>

</style>
