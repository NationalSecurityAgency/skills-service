<template>
  <div class="modal-card" style="width: auto">
    <header class="modal-card-head">
      New Level
    </header>

    <section class="modal-card-body">
      <b-field label="Percent">
        <b-input v-model="percent" required></b-input>
      </b-field>
    </section>

    <footer class="modal-card-foot">
      <button class="button is-link is-outlined" v-on:click="$parent.close()">
        <span>Cancel</span>
        <span class="icon is-small">
              <i class="fas fa-stop-circle"/>
            </span>
      </button>

      <button class="button is-primary is-outlined" v-on:click="createNewLevel">
        <span>Save</span>
        <span class="icon is-small">
              <i class="fas fa-arrow-circle-right"/>
            </span>
      </button>
    </footer>
  </div>
</template>

<script>
  export default {
    name: 'NewLevel',
    methods: {
      createNewLevel() {
        this.$emit('new-level', this.percent);
        this.$parent.close();
      },
    },
  };
</script>

<style scoped>

</style>
