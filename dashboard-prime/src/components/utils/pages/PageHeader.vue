<script setup>
import SkillsSpinner from '@/components/utils/SkillsSpinner.vue'

const props = defineProps(['loading', 'options'])

</script>

<template>
  <div data-cy="pageHeader">
    <skills-spinner v-if="loading" :is-loading="loading" />
    <div v-if="!loading" class="flex py-1 px-1 w-full">
      <slot name="banner"></slot>
      <div class="flex w-full flex-wrap">
        <div class="mt-2 text-center lg:text-left w-full lg:w-auto">
          <div class="text-2xl">
            <i v-if="options.icon" class="mr-2" :class="options.icon" />
            <span data-cy="title">{{ options.title }}</span>
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
            <div v-for="(stat) in options.stats" :key="stat.label" data-cy="pageHeaderStat" class="w-full md:w-6 lg:w-auto">
              <Card class="ml-3 mt-2">
                <template #content>
                  <div class="flex">
                    <div class="" :data-cy="`pageHeaderStat_${stat.label}`">
                      <div class="uppercase text-color-secondary">{{ stat.label }}</div>
                      <div class="font-bold text-xl">
                        <span v-if="stat.preformatted" data-cy="statPreformatted" v-html="stat.preformatted" />
                        <span v-else data-cy="statValue">{{ stat.count }}</span>
                      </div>

                      <span v-if="stat.warnMsg" class="ml-1">
                            <i class="fa fa-exclamation-circle text-warning"
                               :aria-label="`Warning: ${stat.warnMsg}`"
                               role="alert"
                               v-tooltip="stat.warnMsg" />
                          </span>
                    </div>
                    <div class="ml-3 flex-1 text-right">
                      <i :class="stat.icon" style="font-size: 2.2rem;"></i>
                    </div>
                  </div>
                  <div class="text-left" style="font-size:0.9rem;" v-if="stat.secondaryPreformatted"
                       v-html="stat.secondaryPreformatted"
                       :data-cy="`pageHeaderStatSecondaryLabel_${stat.label}`"></div>
                  <div v-if="stat.secondaryStats">
                    <div v-for="secCount in stat.secondaryStats" :key="secCount.label">
                      <div v-if="secCount.count > 0" style="font-size: 0.9rem"
                           class="text-left">
                        <Badge :severity="`${secCount.badgeVariant}`" value="{{ secCount.count }}"
                               :data-cy="`pageHeaderStats_${stat.label}_${secCount.label}`">
                          <span></span>
                        </Badge>
                        <span class="text-left text-secondary text-uppercase ml-1"
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
