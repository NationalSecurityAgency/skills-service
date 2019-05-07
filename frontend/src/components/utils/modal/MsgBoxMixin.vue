<script>
  export default {
    name: 'MsgBoxMixin',
    methods: {
      msgConfirm(msg, overallTitle = 'Please Confirm!', okButtonTitle = 'YES, Delete It!') {
        return this.$bvModal.msgBoxConfirm(msg, {
          title: overallTitle,
          size: 'sm',
          buttonSize: 'sm',
          okVariant: 'danger',
          okTitle: okButtonTitle,
          cancelTitle: 'Cancel',
          footerClass: 'p-2',
          hideHeaderClose: false,
          centered: true,
        });
      },
    },
  };
</script>

<style scoped>

</style>
