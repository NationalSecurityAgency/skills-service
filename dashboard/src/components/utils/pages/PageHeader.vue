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
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'
import { useNumberFormat } from '@/common-components/filter/UseNumberFormat.js'
import { useColors } from '@/skills-display/components/utilities/UseColors.js'
import { onMounted, onUnmounted, ref } from 'vue'
import { useLayoutSizesState } from '@/stores/UseLayoutSizesState.js'

defineProps(['loading', 'options'])
const numberFormat = useNumberFormat()
const colors = useColors()
const layoutSizes = useLayoutSizesState()

const pageHeader = ref(null)
const handleResize = () => {
  layoutSizes.updatePageHeaderWidth(pageHeader.value.getBoundingClientRect().width)
}
onMounted(() => {
  handleResize()
  window.addEventListener('resize', handleResize);
})
onUnmounted(() => {
  window.removeEventListener('resize', handleResize);
})

</script>

<template>
  <div ref="pageHeader">
    <Card data-cy="pageHeader" class="mt-2" role="heading" aria-level="1">
      <template #content>
        <skills-spinner v-if="loading" :is-loading="loading"/>
        <div v-if="!loading">
          <slot name="banner"></slot>
          <div class="flex py-1 px-1 w-full">
            <div class="flex w-full flex-wrap">
              <div class="mt-2 text-center lg:text-left w-full lg:w-auto">
                <div class="text-2xl flex">
                  <Avatar v-if="options.icon" class="mr-2" :icon="options.icon"/>
                  <h1 class="text-2xl my-0 font-normal" data-cy="title" style="overflow-wrap: anywhere;">
                    {{ options.title }}</h1>
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
                <div class="flex justify-center lg:justify-end flex-wrap">
                  <div v-for="(stat, index) in options.stats" :key="stat.label" data-cy="pageHeaderStat"
                       class="w-full md:w-6/12 lg:w-auto mb-1">
                    <Card :class="{ 'md:ml-4' : index % 2 === 1, 'lg:ml-4' : index !== 0 }" class="mt-2 h-full" :pt="{ body: { class: 'p-3' }, content: { class: 'p-0' } }">
                      <template #content>
                        <div class="flex">
                          <div class="" :data-cy="`pageHeaderStat_${stat.label}`">
                            <div class="uppercase text-muted-color">{{ stat.label }}</div>
                            <div class="font-bold text-xl">
                              <span v-if="stat.preformatted" data-cy="statPreformatted" v-html="stat.preformatted"/>
                              <span v-else data-cy="statValue">{{ numberFormat.pretty(stat.count) }}</span>
                            </div>
                          </div>
                          <div class="ml-4 flex-1 text-right">
                            <i :class="`${stat.icon} ${colors.getTextClass(index)}`" style="font-size: 2.2rem;"
                               aria-hidden="true"></i>
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
    </Card>
  </div>
</template>

<style scoped>

</style>
