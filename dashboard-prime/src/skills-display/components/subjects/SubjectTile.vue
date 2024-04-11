<script setup>
const props = defineProps({
  subject: {
    type: Object,
    required: true,
  },
})
</script>

<template>
  <Card class="h-full">
    <template #content>
      <div>
        {{ subject.subject }}
      </div>
    </template>
    <template #footer>
      <router-link
        :to="{ name:'SubjectDetailsPage', params: { subjectId: subject.subjectId } }"
        :aria-label="`Click to navigate to the ${subject.subject} subject page.`"
        data-cy="subjectTileBtn">
        <Button
          label="View"
          icon="far fa-eye"
          outlined class="w-full" size="small"/>
      </router-link>
    </template>
  </Card>
</template>

<style scoped>

</style>