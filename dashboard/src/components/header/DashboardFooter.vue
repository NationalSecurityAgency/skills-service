<template>
<footer class="bg-primary page-footer font-small text-white-50 p-3 px-4 border-top border-info">
  <div class="row">
    <div class="col">
      <img src="/img/skilltree_logo_arrows.png" style="height: 1.5rem; font-family: Carlito " />
      <span class="ml-2 small">SkillTree Dashboard</span>
    </div>
    <div class="col text-right">
      <span class="small mr-2">v{{ $store.getters.config.dashboardVersion }}</span>
      <i class="fas fa-code-branch"></i>
    </div>
  </div>
</footer>
</template>

<script>
  export default {
    name: 'DashboardFooter',
  };
</script>

<style scoped>

</style>
