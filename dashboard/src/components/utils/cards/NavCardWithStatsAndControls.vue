/*
Copyright 2020 SkillTree

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
<template>
  <div class="card h-100">
    <div class="card-body d-flex flex-column">
      <div class="row mb-2 nav-cards-header">
        <div class="col">
          <div class="media">
            <router-link v-if="options.icon" tag="a"
                         :to="options.navTo" aria-label="Navigate to Skills" data-cy="iconLink" aria-hidden="true"
                         tabindex="-1">
              <div class="d-inline-block mr-2 border rounded text-info text-center icon-link" style="min-width: 3.2rem;"
                   aria-hidden="true">
                <i :class="[`${options.icon}`]" class="m-1" aria-hidden="true"/>
              </div>
            </router-link>
            <div class="media-body" style="min-width: 0px;">
              <div class="text-truncate text-info mb-0 pb-0 preview-card-title">
                <router-link v-if="options.icon" tag="a" :to="options.navTo" data-cy="titleLink">{{
                    options.title
                  }}
                </router-link>
                <i v-if="options.warn" class="fas fa-exclamation-circle text-warning ml-1"
                   style="font-size: 1.5rem;"
                   role="alert"
                   :aria-label="`Warning: ${options.warnMsg}`"
                   v-b-tooltip.hover="options.warnMsg"/>
              </div>
              <div class="text-truncate text-secondary preview-card-subTitle" data-cy="subTitle">{{ options.subTitle }}</div>
            </div>
          </div>
        </div>
      </div>

      <div class="mt-3">
        <slot name="underTitle"></slot>
      </div>

        <div class="row text-center justify-content-center flex-grow-1">
          <div v-for="(stat) in options.stats" :key="stat.label" class="col my-3" style="min-width: 10rem;">
            <div :data-cy="`pagePreviewCardStat_${stat.label}`"
                 class="border rounded stat-card h-100">
              <i :class="stat.icon"></i>
              <p class="text-uppercase text-muted count-label">{{ stat.label }}</p>
              <strong class="h4" data-cy="statNum">{{ stat.count | number }}</strong>
              <i v-if="stat.warn" class="fas fa-exclamation-circle text-warning ml-1"
                 style="font-size: 1.5rem;"
                 v-b-tooltip.hover="stat.warnMsg"
                 role="alert"
                 :aria-label="`Warning: ${stat.warnMsg}`"
                 data-cy="warning"/>
              <div v-if="stat.secondaryStats">
                <div v-for="secCount in stat.secondaryStats" :key="secCount.label">
                  <div v-if="secCount.count > 0" style="font-size: 0.9rem">
                    <b-badge :variant="`${secCount.badgeVariant}`"
                             :data-cy="`pagePreviewCardStat_${stat.label}_${secCount.label}`">
                      <span>{{ secCount.count }}</span>
                    </b-badge>
                    <span class="text-left text-secondary text-uppercase ml-1"
                          style="font-size: 0.8rem">{{ secCount.label }}</span>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <div>
          <slot name="footer"></slot>
        </div>

      <div v-if="!disableSortControl && !isReadOnlyProj"
           ref="sortControl"
           @mouseover="overSortControl = true"
           @mouseleave="overSortControl = false"
           @keyup.down="moveDown"
           @keyup.up="moveUp"
           @click.prevent.self
           class="position-absolute text-secondary px-2 py-1 sort-control"
           tabindex="0"
           :aria-label="`Sort Control. Current position for ${options.title} is ${options.displayOrder}. Press up or down to change the order.`"
           role="button"
           data-cy="sortControlHandle"><i class="fas fa-arrows-alt"></i></div>
    </div>
  </div>
</template>

<script>
  import ProjConfigMixin from '@/components/projects/ProjConfigMixin';

  export default {
    name: 'NavCardWithStatsAndControls',
    mixins: [ProjConfigMixin],
    props: {
      options: {
        icon: String,
        title: String,
        warn: Boolean,
        warnMsg: String,
        subTitle: String,
        stats: {},
        displayOrder: Number,
      },
      disableSortControl: {
        type: Boolean,
        default: false,
      },
    },
    data() {
      return {
        overSortControl: false,
      };
    },
    methods: {
      moveDown() {
        this.$emit('sort-changed-requested', {
          direction: 'down',
        });
      },
      moveUp() {
        this.$emit('sort-changed-requested', {
          direction: 'up',
        });
      },
      focusSortControl() {
        this.$refs.sortControl.focus();
      },
    },
  };
</script>

<style lang="scss" scoped>
  @import "../../../assets/custom";

  .preview-card-title {
    font-size: 1.4rem;
    font-weight: bold;
  }

  .preview-card-subTitle {
    max-width: 12rem;
    font-size: 0.8rem;
  }

  .count-label {
    font-size: 0.9rem;
  }

  .nav-cards-header i {
    font-size: 1.8rem;
    display: inline-block;
  }

  .stat-card {
    background-color: #f8f9fa;
    padding: 1rem;
  }

  .icon-link:hover {
    border-color: black !important;
  }

  .sort-control {
    font-size: 1.3rem !important;
    color: #b3b3b3 !important;
    top: 0rem;
    right: 0rem;
    border-bottom: 1px solid #e8e8e8;
    border-left: 1px solid #e8e8e8;
    background-color: #fbfbfb !important;
    border-bottom-left-radius:.25rem!important
  }

  .sort-control:hover, .sort-control i:hover {
    cursor: grab !important;
    color: $info !important;
    font-size: 1.5rem;
  }
</style>
