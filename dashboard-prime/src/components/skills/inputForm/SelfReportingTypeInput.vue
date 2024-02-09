<script setup>
import { ref } from 'vue'

const enabled = ref(false)

const selectedCategory = ref('Production');
const categories = ref([
  { name: 'Approval Queue', key: 'A' },
  { name: 'Honor System', key: 'M' },
  { name: 'Quiz/Survey', key: 'P' },
  { name: 'Video', key: 'R' }
]);
</script>

<template>
  <div class="flex">
    <div>
      <div class="text-xl font-bold class mb-2">Self Reporting</div>
      <ToggleButton
        v-model="enabled"
        onIcon="fas fa-check"
        offIcon="fas fa-times"
        class="w-full sm:w-10rem py-1 px-3 h-2rem"
        onLabel="Enabled"
        offLabel="Disabled" />
    </div>



      <div class="card flex ml-4">
        <div class="flex flex-column gap-3">
          <div v-for="category in categories" :key="category.key" class="flex align-items-center">
            <RadioButton v-model="selectedCategory"
                         :disabled="!enabled"
                         :inputId="category.key" name="dynamic" :value="category.name" />
            <label :for="category.key" class="ml-2">{{ category.name }}</label>
          </div>
        </div>
      </div>


  </div>
</template>

<style scoped>

</style>