<template>
  <div
    :class="{ 'badgeAchieved': badge.badgeAchieved }"
    class="badge-tile">
    <i
      v-if="badge.badgeAchieved"
      class="fas fa-check-circle completed-indicator" />
    <i
      v-if="badge.gem"
      class="fas fa-gem gem-indicator" />
    <i
      :class="badge.iconClass"
      class="badge-icon" />
    <p>{{ badge.badge }}</p>
  </div>
</template>

<script>
  export default {
    props: {
      badge: {
        type: Object,
        required: true,
      },
    },
  };
</script>

<style type="scss" scoped>
  .badge-tile {
    cursor: pointer;
    width: 130px;
    margin: 10px;
    padding: 5px;
    position: relative;
    background-color: white;
    white-space: normal;
    opacity: 0.5;
  }

  .badge-tile.badgeAchieved {
    opacity: 1;
  }

  .badge-tile:hover {
    background-color: #f8f8f8;
  }

  .badge-tile .badge-icon {
    color: gold;
    font-size: 80px;
  }

  .badge-tile p {
    padding-top: 5px;
    margin-bottom: 0;
    color: #888;
    font-size: 17px;
    font-weight: bold;
  }

  .completed-indicator {
    color: green;
    position: absolute;
    font-size: 19px;
    left: 15px;
    top: -1px;
  }

  .gem-indicator {
    color: red;
    position: absolute;
    right: 15px;
    top: 50%;
    font-size: 25px;
  }
</style>
