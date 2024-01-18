<script setup>
import { computed } from 'vue';
import Badge from 'primevue/badge';
import LoadingContainer from '../LoadingContainer.vue';

const props = defineProps(['loading', 'options'])

const titleCss = computed(() => {
  const statCount = props.options?.stats ? props.options.stats.length : 0;
  return {
    pageHeaderTitle: true,
    'text-center': true,
    'text-lg-left': true,
    'col-lg-5': statCount > 2,
    'col-xxxl-3': statCount > 2,
    'col-lg-7': statCount === 2,
    'col-xxxl-8': statCount === 2,
    'col-lg-8': statCount < 2,
    'col-xl-9': statCount < 2,
    'col-xxxl-9': statCount < 2,
  };
});

const statcsCss = computed(() => {
  const statCount = props.options?.stats ? props.options.stats.length : 0;
  return {
    'col-lg-7': statCount > 2,
    'col-xxxl-9': statCount > 2,
    'col-lg-5': statCount === 2,
    'col-xxxl-4': statCount === 2,
    'col-lg-4': statCount < 2,
    'col-xl-3': statCount < 2,
    'col-xxxl-3': statCount < 2,

  };
});

const individualStatCss = computed(() => {
  const statCount = props.options?.stats ? props.options.stats.length : 0;
  return {
    'col-md-6': statCount >= 2,
    'col-xl-4': statCount >= 2,
    'col-xxxl-2': statCount >= 2,
    'col-xl-6': statCount === 2,
    'col-xxxl-6': statCount === 2,
    'col-md-12': statCount < 2,
    'col-xl-12': statCount < 2,
    'col-xxxl-12': statCount < 2,
    'mt-2': true,
  };
});
</script>

<template>
  <div class="mx-0 py-0" data-cy="pageHeader">
    <div class="px-1">
      <LoadingContainer :is-loading="loading">
        <div class="flex-auto">
          <slot name="banner"></slot>
          <div class="grid">
            <div :class="titleCss">
              <div class="text-3xl"><i v-if="options.icon" class="has-text-link" :class="options.icon"/> {{ options.title }}<slot name="right-of-header"></slot></div>
              <slot name="subTitle"><div class="text-xl text-500">{{ options.subTitle }}</div></slot>
              <slot name="subSubTitle"></slot>
            </div>
            <div :class="statcsCss">
              <div class="grid text-center mt-4 mt-lg-0 justify-content-center justify-content-lg-end">
                <div v-for="(stat) in options.stats" :key="stat.label" :class="individualStatCss" data-cy="pageHeaderStat">
                  <div class="card h-100" >
                    <div class="card-body">
                      <div class="d-flex flex-row">
                        <div class="text-left mr-auto" :data-cy="`pageHeaderStat_${stat.label}`">
                          <div class="h5 card-title text-uppercase text-muted mb-0 small">{{stat.label}}</div>
                          <div v-if="stat.preformatted" data-cy="statPreformatted" v-html="stat.preformatted"/>
                          <span v-else class="h5 font-weight-bold mb-0" data-cy="statValue">{{ stat.count | number}}</span>
                          <span v-if="stat.warnMsg" class="ml-1">
                            <i class="fa fa-exclamation-circle text-warning"
                               :aria-label="`Warning: ${stat.warnMsg}`"
                               role="alert"
                               v-tooltip="stat.warnMsg" />
                          </span>
                        </div>
                        <div class="">
                          <i :class="stat.icon" style="font-size: 2.2rem;"></i>
                        </div>
                      </div>
                      <div class="text-left" style="font-size:0.9rem;" v-if="stat.secondaryPreformatted" v-html="stat.secondaryPreformatted" :data-cy="`pageHeaderStatSecondaryLabel_${stat.label}`"></div>
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
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
          <slot name="footer"></slot>
        </div>
      </LoadingContainer>
    </div>
  </div>
</template>

<style scoped>
.pageHeaderTitle {
  overflow-wrap: break-word;
}
</style>
