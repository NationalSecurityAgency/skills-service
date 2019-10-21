<script>
  export default {
    data() {
      return {
        requestId: 0,
        completedRequestId: 0,
      };
    },
    methods: {
      getRequestId() {
        this.requestId = this.requestId + 1;
        return this.requestId;
      },
      ensureOrderlyResultHandling(requestId, closure) {
        if (requestId > this.completedRequestId) {
          this.completedRequestId = requestId;
          closure();
        }
      },
    },
  };
</script>
