<template functional>
  <div>
    <label class="skill-tile-label">Points Progress</label>
    <div class="user-skills-history-no-data">
      <span class="fa-stack user-skills-no-data-icon">
        <i class="fas fa-chart-bar fa-stack-2x"/>
        <strong class="fa-stack-1x user-skills-no-data-icon-text">Locked</strong>
      </span>
    </div>
    <div class="user-skills-no-data-icon-subtext">
    Not enough data is available. Earn more points <br> to unlock the points progress chart.
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
    font-size: 75px;
    opacity: .5;
    width: 100%;
  }

  .user-skills-no-data-icon-text {
    font-size: 0.4em;
    font-weight: 700;
    background-color: #eaeaea;
    color: #f15252;
    opacity: 0.9;
    line-height: 1.1em;
    margin-top: 1.5em;
  }

  .user-skills-no-data-icon-subtext {
    margin-top: 5px;
    font-size: 15px;
    font-weight: 700;
    display: block;
  }
</style>
