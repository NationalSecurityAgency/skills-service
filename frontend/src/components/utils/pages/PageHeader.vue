<template>
  <div class="card">
    <div class="card-body">
      <loading-container :is-loading="loading">
        <div class="row">
          <div class="col-lg-6 text-center text-lg-left">
            <h1 class="h3"><i v-if="options.icon" class="has-text-link" :class="options.icon"/> {{ options.title }}</h1>
            <h2 class="text-muted">{{ options.subTitle }}</h2>
          </div>
          <div class="col-lg-6">
            <div class="row text-center mt-4 mt-lg-0 justify-content-end">
              <div v-for="(stat) in options.stats" :key="stat.label" class="col-6 col-sm-3">
                <div>
                  <p class="h6 text-uppercase text-muted">{{stat.label}}</p>
                  <p class="h2">{{ stat.count | number}}
                    <b-tooltip v-if="stat.warnMsg" :label="stat.warnMsg"
                               position="is-bottom" animanted="true" type="is-light" multilined>
                      <i class="fa fa-exclamation-circle text-warning"></i>
                    </b-tooltip>
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
        defaut: true,
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
