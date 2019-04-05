<template functional>
  <div class="card skills-full-height">
    <div class="card-header">
      <h6 class="card-title mb-0 float-left">Points Progress</h6>
    </div>
    <div class="card-body">
      <span class="fa-stack user-skills-no-data-icon">
        <i class="fas fa-chart-line fa-stack-2x"/>
        <strong class="fa-stack-1x user-skills-no-data-icon-text">Locked</strong>
      </span>
      <div class="user-skills-no-data-icon-subtext">*** Earn more points to unlock points progress chart! ***</div>
    </div>
  </div>
</template>

<style scoped>
  .user-skills-history-no-data {
    width: 100%;
    padding: 10px 0;
    background-color: #eaeaea;
    border-radius: 10px;
  }

  .user-skills-no-data-icon {
    display: block;
    font-size: 70px;
    opacity: .5;
    width: 100%;
  }

  .user-skills-no-data-icon-text {
    font-size: 0.3em;
    font-weight: 700;
    background-color: #eaeaea;
    color: #f10d1a;
    opacity: 0.8;
    line-height: 1.1em;
    margin-top: 2.5em;
  }

  .user-skills-no-data-icon-subtext {
    font-size: 15px;
    font-size: 0.9em;
    display: block;
  }
</style>
