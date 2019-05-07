<template>
  <div class="form-group mb-0">
    <label>{{ label }}</label>
    <input type="text" class="form-control" v-model="internalValue" :v-focus="focus" @input="dataChanged">
  </div>
</template>

<script>
  export default {
    name: 'FormTextInput',
    props: {
      label: String,
      value: String,
      focus: Boolean,
    },
    data() {
      return {
        internalValue: this.value,
      };
    },
    methods: {
      dataChanged() {
        this.$emit('input', this.internalValue);
      },
    },
  };
</script>

<style scoped>

</style>
