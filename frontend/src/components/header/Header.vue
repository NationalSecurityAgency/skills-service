<template>
  <div>
    <div class="row p-3">
      <div class="col-12 col-md text-center text-md-left">
        <router-link class="h2 text-primary" to="/"><i class="fas fa-users"/> User Skills</router-link>
      </div>

      <div class="col-12 col-md text-center text-md-right">
        <inception-button class="mr-2 mt-3 mt-md-0"></inception-button>
        <settings-button class="mr-2 mt-3 mt-md-0"/>
        <help-button class="mt-3 mt-md-0"/>
      </div>
    </div>
    <breadcrumb></breadcrumb>
  </div>
</template>

<script>
  import SettingsButton from './SettingsButton';
  import Breadcrumb from './Breadcrumb';
  import InceptionButton from '../inception/InceptionButton';
  import HelpButton from './HelpButton';

  export default {
    name: 'Header',
    components: {
      HelpButton,
      InceptionButton,
      Breadcrumb,
      SettingsButton,
    },
  };
</script>

<style scoped>

</style>
