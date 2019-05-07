<template>
  <div class="card h-100">
    <div class="card-body">
      <div class="media">
        <div class="d-inline-block mt-1 mr-3">
          <i class="fa-3x" :class="iconClass"/>
        </div>
        <div class="media-body">
          <h4 class="mb-2 text-uppercase">{{ title }}</h4>
          <div style="font-size: 0.95rem;" class="text-secondary">
            <slot>
              {{ subTitle }}
            </slot>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'MediaInfoCard',
    props: {
      title: String,
      subTitle: String,
      iconClass: String,
    },
  };
</script>

<style scoped>

</style>
