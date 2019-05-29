<template>
  <b-button to="/inception" variant="outline-info">
    Level
    <b-spinner v-if="level === -1" small label="Spinning"></b-spinner>
    <span v-else>{{ level }}</span>
  </b-button>
</template>

<script>
  import LevelService from '../levels/LevelService';

  export default {
    name: 'InceptionButton',
    data() {
      return {
        level: -1,
      };
    },
    mounted() {
      LevelService.getUserLevel('Inception', this.userId)
        .then((res) => {
          this.level = res;
        });
    },
    computed: {
      userId() {
        return this.$store.getters.userInfo.userId;
      },
    },
  };
</script>

<style scoped>

</style>
