<template>
  <span class="mx-3 mr-4 message-center" v-b-tooltip.hover="'You have 3 critical alerts to review'">
    <span class="text-primary msg-container mr-1" data-count="3" style="font-size: 0.8rem">
      <i class="fas fa-bell" style="font-size: 1.55rem"></i>
    </span>
  </span>
</template>

<script>
  export default {
    name: 'MessageCenter',
  };
</script>

<style scoped>
.message-center:hover {
  cursor: pointer;
}
.msg-container {
  display: inline-block;
  height: 2em;
  line-height: 2em;
  position: relative;
  vertical-align: middle;
  width: 2.5em;
}

.msg-container[data-count]:after{
  position:absolute;
  right:-10%;
  top:-1%;
  content: attr(data-count);
  font-size:60%;
  padding:.6em;
  border-radius:999px;
  line-height:.75em;
  color: white;
  background:rgba(255,0,0,.85);
  text-align:center;
  min-width:2em;
  font-weight:bold;
}

.fa-stack[data-count]:after{
  position:absolute;
  right:-10%;
  top:-1%;
  content: attr(data-count);
  font-size:60%;
  padding:.6em;
  border-radius:999px;
  line-height:.75em;
  color: white;
  background:rgba(255,0,0,.85);
  text-align:center;
  min-width:2em;
  font-weight:bold;
}
</style>
