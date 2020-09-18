<template>
  <div>
  <span v-if="achievement.type === types[0]">
            <span class="border border-info rounded d-inline-block bg-white p-1 text-center icon">
              <i class="fa fa-trophy text-muted"/>
            </span>
            <span class="text-primary text-uppercase ml-2">Overall</span> <span
    class="text-secondary ml-2">Level:</span> <strong>{{ achievement.level }}</strong>
          </span>
    <span v-if="achievement.type === types[1]">
      <span class="border border-info rounded d-inline-block bg-white p-1 text-center icon">
            <i class="fa fa-cubes text-muted"/>
      </span>
            <span class="text-secondary ml-2">Subject:</span> <span class="text-info">{{ achievement.name }}</span>
            <span class="text-secondary ml-2">Level:</span> <strong>{{ achievement.level }}</strong>
          </span>
    <span v-if="achievement.type === types[2]">
      <span class="border border-info rounded d-inline-block bg-white p-1 text-center icon">
            <i class="fa fa-graduation-cap text-muted"/>
      </span>
            <span class="text-secondary ml-2">Skill:</span> <strong class="text-info">{{ achievement.name }}</strong>
          </span>
    <span v-if="achievement.type === types[3]">
      <span class="border border-info rounded d-inline-block bg-white p-1 text-center icon">
            <i class="fa fa-award text-muted"/>
      </span>
            <span class="text-secondary ml-2">Badge:</span> <strong class="text-info">{{ achievement.name }}</strong>
          </span>
    <span v-if="isUnknownType">
      <span class="border border-info rounded d-inline-block bg-white p-1 text-center icon">
            <i class="fa fa-award text-muted"/>
    </span>
            <span class="text-secondary ml-2">Name:</span> <span class="text-info">{{ achievement.name }}</span>
            <span v-if="achievement.level" class="text-secondary ml-2">Level:</span> <span
      class="text-info">{{ achievement.level }}</span>
          </span>
  </div>
</template>

<script>
  export default {
    name: 'AnAchievement',
    props: ['achievement'],
    data() {
      return {
        types: [
          'Overall',
          'Subject',
          'Skill',
          'Badge',
        ],
      };
    },
    computed: {
      isUnknownType() {
        return this.types.find((item) => item === this.achievement.type) === undefined;
      },
    },
  };
</script>

<style scoped>
.icon {
  width: 2rem;
}
</style>
