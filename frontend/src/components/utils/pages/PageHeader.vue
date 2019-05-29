<template>
  <div class="card mb-3 mx-0 py-0">
    <div class="card-body">
      <loading-container :is-loading="loading">
        <div class="row">
          <div class="col-lg-6 text-center text-lg-left">
            <h3><i v-if="options.icon" class="has-text-link" :class="options.icon"/> {{ options.title }}</h3>
            <h5 class="text-muted">{{ options.subTitle }}</h5>
          </div>
          <div class="col-lg-6">
            <div class="row text-center mt-4 mt-lg-0 justify-content-end">
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
