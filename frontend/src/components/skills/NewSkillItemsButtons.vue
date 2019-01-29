<template>
  <div>
    <a v-on:click="newSkill(false)" class="button is-outlined is-info">
      <span>Skill</span>
      <span class="icon is-small">
            <i class="fas fa-plus-circle"/>
      </span>
    </a>
  </div>
</template>

<script>
  export default {
    name: 'NewSkillItemsButtons',
    methods: {
      newSkill() {
        this.$emit('new-skill-item', true);
      },
    },
  };
</script>

<style scoped>

</style>
