<template>
  <b-button v-if="level > -1" to="/inception" variant="outline-info">
    Level {{ level }}
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
