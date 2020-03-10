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
  <div class="card mb-3 mx-0 py-0">
    <div class="card-body">
      <loading-container :is-loading="loading">
        <div class="row">
          <div class="col-lg-5 text-center text-lg-left">
            <h3><i v-if="options.icon" class="has-text-link" :class="options.icon"/> {{ options.title }}<slot name="right-of-header"></slot></h3>
            <h5 class="text-muted">{{ options.subTitle }}</h5>
          </div>
          <div class="col-lg-7">
            <div class="row text-center mt-4 mt-lg-0 justify-content-center justify-content-lg-end">
              <div v-for="(stat) in options.stats" :key="stat.label" class="col-6 col-sm-3">
                <div>
                  <p class="h6 text-uppercase text-muted">{{stat.label}}</p>
                  <p class="h2">{{ stat.count | number}}
                    <span v-if="stat.warnMsg">
                      <i class="fa fa-exclamation-circle text-warning" v-b-tooltip.hover="stat.warnMsg"/>
                    </span>
                  </p>
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

</style>
