<template>
    <div v-if=projectLevel>
        <div class="row">
            <div class="col-lg-2 my-1">
<!--                <i class="text-success fa fa-trophy fa-3x float-left mr-2" style="min-width: 3rem;, max-width: 4rem;"></i>-->
                <div class="text-center text-nowrap">
                    <h5>Requires</h5>
                    <h4> Level {{ projectLevel.requiredLevel }}</h4>
                </div>
            </div>
            <div class="col-lg-10 text-sm-left text-secondary text-center skills-text-description">
                <div class="row">
                    <div class="col text-right">
                        <small class=" float-right text-navy" :class="{ 'text-success': percent === 100 }">
                            <i v-if="percent === 100" class="fa fa-check"/> {{ percent }}% Complete
                        </small>
                    </div>

                </div>
                <div class="mb-2">
                    <vertical-progress v-if="percent === 100"
                                       total-progress-bar-color="#59ad52" before-today-bar-color="#59ad52"
                                       :total-progress="percent"
                                       :total-progress-before-today="0" :is-locked="false"
                                       :is-clickable="false"/>
                    <vertical-progress v-if="percent !== 100"
                                       :total-progress="percent"
                                       :total-progress-before-today="0" :is-locked="false"
                                       :is-clickable="false"/>
                </div>
            </div>
        </div>
    </div>
</template>

<script>
  import VerticalProgress from '@/common/progress/VerticalProgress.vue';

  export default {
    name: 'BadgeDetailsOverview',
    components: {
      VerticalProgress,
    },
    props: {
      projectLevel: {
        type: Object,
      },
    },
    computed: {
      percent() {
        if (this.projectLevel.achievedLevel >= this.projectLevel.requiredLevel) {
          return 100;
        }
        return Math.trunc((this.projectLevel.achievedLevel / this.projectLevel.requiredLevel) * 100);
      },
    },
  };
</script>

<style scoped>

</style>
