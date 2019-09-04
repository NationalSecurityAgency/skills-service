<script>
  export default {
    name: 'MsgBoxMixin',
    methods: {
      msgConfirm(msg, overallTitle = 'Please Confirm!', okButtonTitle = 'YES, Delete It!') {
        return this.$bvModal.msgBoxConfirm(msg, {
          title: overallTitle,
          size: 'md',
          buttonSize: 'sm',
          okVariant: 'danger',
          okTitle: okButtonTitle,
          cancelTitle: 'Cancel',
          footerClass: 'p-2',
          hideHeaderClose: false,
          centered: true,
        });
      },
      msgOk(msg, overallTitle = 'Message!', okButtonTitle = 'Ok') {
        return this.$bvModal.msgBoxOk(msg, {
          title: overallTitle,
          size: 'sm',
          buttonSize: 'sm',
          okTitle: okButtonTitle,
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
