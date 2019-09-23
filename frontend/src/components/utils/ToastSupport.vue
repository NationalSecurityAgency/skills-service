<script>
  export default {
    name: 'ToastSupport',
    methods: {
      successToast(toastTitle, msg) {
        this.toast(toastTitle, msg);
      },
      errorToast(toastTitle, msg) {
        this.toast(toastTitle, msg, 'danger');
      },
      toast(toastTitle, msg, toastVairant = 'success') {
        this.$bvToast.toast(msg, {
          title: toastTitle,
          autoHideDelay: 4000,
          toaster: 'b-toaster-top-right',
          solid: true,
          appendToast: true,
          variant: toastVairant,
        });
      },
    },
  };
</script>

<style scoped>

</style>
