<template>
  <b-tab-item :label="setting.category"/>
</template>

<script>
  import BTabItem from 'buefy/src/components/tabs/TabItem';

  export default {
    name: 'SettingItem',
    components: { BTabItem },
    props: {
      setting: Object,
    },
  };
</script>

<style scoped>

</style>
