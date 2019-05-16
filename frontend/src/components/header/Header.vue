<template>
  <div>
    <div class="row p-3">
      <div class="col-sm text-center text-sm-left">
        <router-link class="h2 text-primary" to="/"><i class="fas fa-users"/> User Skills</router-link>
      </div>

      <div class="col text-center text-sm-right mt-3 mt-sm-0">
        <settings-button class="mr-2"/>
      </div>
    </div>
    <breadcrumb></breadcrumb>
  </div>
</template>

<script>
  import SettingsButton from './SettingsButton';
  import Breadcrumb from './Breadcrumb';

  export default {
    name: 'Header',
    components: {
      Breadcrumb,
      SettingsButton,
    },
  };
</script>

<style scoped>

</style>
