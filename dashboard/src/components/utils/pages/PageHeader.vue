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
  <div class="mx-0 py-0 bg-white" data-cy="pageHeader">
    <div class="card-body px-1">
      <loading-container :is-loading="loading">
        <div class="container-fluid">
          <div class="row">
            <div class="pageHeaderTitle col-lg-5 col-xxxl-3 text-center text-lg-left">
              <h3><i v-if="options.icon" class="has-text-link" :class="options.icon"/> {{ options.title }}<slot name="right-of-header"></slot></h3>
              <div class="h5 text-muted">{{ options.subTitle }}</div>
              <slot name="subSubTitle"></slot>
            </div>
            <div class="col-lg-7 col-xxxl-9">
              <div class="row text-center mt-4 mt-lg-0 justify-content-center justify-content-lg-end">
                <div v-for="(stat) in options.stats" :key="stat.label" class="col-md-6 col-xl-4 col-xxxl-2 mt-2" data-cy="pageHeaderStat">
                  <div class="card h-100" >
                    <div class="card-body">
                      <div class="d-flex flex-row">
                        <div class="text-left mr-auto" :data-cy="`pageHeaderStat_${stat.label}`">
                          <div class="h5 card-title text-uppercase text-muted mb-0 small">{{stat.label}}</div>
                          <span class="h5 font-weight-bold mb-0">{{ stat.count | number}}</span>
                          <span v-if="stat.warnMsg" class="ml-1">
                            <i class="fa fa-exclamation-circle text-warning" v-b-tooltip.hover="stat.warnMsg"/>
                          </span>
                        </div>
                        <div class="">
                          <i :class="stat.icon" style="font-size: 2.2rem;"></i>
                        </div>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>
      </loading-container>
    </div>
  </div>
</template>

<script>
  import LoadingContainer from '../LoadingContainer';

  export default {
    name: 'PageHeader',
    components: {
      LoadingContainer,
    },
    props: {
      loading: {
        type: Boolean,
        default: true,
      },
      options: {
        icon: String,
        title: {
          type: String,
          default: '',
        },
        subTitle: {
          type: String,
          default: '',
        },
        stats: {
          type: Array,
          default: [],
        },
      },
    },
  };
</script>

<style scoped>

.pageHeaderTitle {
  overflow-wrap: break-word;
}

</style>
