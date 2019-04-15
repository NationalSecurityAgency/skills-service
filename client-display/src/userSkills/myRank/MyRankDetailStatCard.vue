<template>
  <div>
    <div class='card position-relative'>
      <div class='card-body'>
        <i :class="iconClass" />
        <div>
          <h1 class='distribution-icon-text'>{{ value | number }}</h1>
          <h4>{{ label }}</h4>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    props: {
      iconClass: String,
      value: Number,
      label: String,
    },
  };
</script>

<style scoped>
  .card .card-body i {
    color: #0fcc15d1;
    opacity: 0.18;
    font-size: 6rem;
    position: absolute;
    left: 0;
    top: 50%;
    right: 0;
    margin: auto;
    top: 50%;
    transform: translateY(-50%);
  }
</style>
