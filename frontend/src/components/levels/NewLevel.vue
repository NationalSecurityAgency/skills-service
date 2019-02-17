<template>
  <div class="modal-card" style="width: auto">
    <header class="modal-card-head">
      New Level
    </header>

    <section class="modal-card-body">
      <b-field v-if="!this.levelAsPoints" label="Percent">
        <b-input v-model="percentInternal" required></b-input>
      </b-field>
      <b-field v-else label="Points">
        <b-input v-model="pointsInternal" required></b-input>
      </b-field>
      <b-field label="Name">
        <b-input v-model="nameInternal"></b-input>
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
    props: ['levelAsPoints', 'percent', 'points', 'name'],
    data() {
      return {
        percentInternal: this.percent,
        pointsInternal: this.points,
        nameInternal: this.name,
      };
    },
    methods: {
      createNewLevel() {
        console.log(`this.levelAsPoints: ${this.levelAsPoints}`);
        this.$emit('new-level', { percent: this.percentInternal, points: this.pointsInternal, name: this.nameInternal });
        this.$parent.close();
      },
    },
  };
</script>

<style scoped>

</style>
