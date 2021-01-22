<template>
  <span>{{ displayNumber | number }}</span>
</template>

<script>
  export default {
    name: 'AnimatedNumber',
    props: {
      num: Number,
    },
    data() {
      return {
        displayNumber: 0,
        interval: false,
      };
    },
    mounted() {
      this.doAnimate(this.num);
    },
    watch: {
      num(val) {
        this.doAnimate(val);
      },
    },
    methods: {
      doAnimate(val) {
        clearInterval(this.interval);

        if (this.num !== this.displayNumber) {
          this.interval = window.setInterval(() => {
            if (this.displayNumber !== val) {
              let change = (val - this.displayNumber) / 10;
              change = change >= 0 ? Math.ceil(change) : Math.floor(change);
              this.displayNumber += change;
            }
          }, 20);
        }
      },
    },
  };
</script>

<style scoped>

</style>
