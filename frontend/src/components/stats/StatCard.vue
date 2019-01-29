<template>
  <div class="card has-text-centered">
    <div class="card-content">
      <div class="media">
        <div class="media-left">
          <figure class="image is-48x48 has-text-info">
            <i :class="this.icon" style="font-size: 48px;"></i>
          </figure>
        </div>
        <div class="media-content">
          <p class="title is-4 has-text-primary">{{ title }}</p>
          <p class="subtitle is-6 has-text-grey-light is-uppercase">{{ subtitle }}</p>
        </div>
      </div>

      <div class="content has-text-left" style="min-height: 5rem;">
        {{ description }}
      </div>
    </div>

    <div class="card-footer">
      <a href="#" class="button card-footer-item is-primary is-outlined">View</a>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'StatCard',
    props: ['title', 'subtitle', 'description', 'icon'],
  };
</script>

<style scoped>

</style>
