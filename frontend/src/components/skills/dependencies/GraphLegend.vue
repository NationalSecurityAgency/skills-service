<template>
  <div class='legend'>
    <div class='legend-title'>Color Legend</div>
    <div class='legend-scale'>
      <ul class='legend-labels'>
        <li v-for="item in items" v-bind:key="item.label">
          <span v-bind:style=' {background: item.color }'></span>{{ item.label }}
        </li>
      </ul>
    </div>
  </div>
</template>

<script>
  export default {
    name: 'GraphLegend',
    props: ['items'],
  };
</script>

<style scoped>
  .legend .legend-title {
    text-align: left;
    margin-bottom: 5px;
    font-weight: bold;
    font-size: 90%;
  }

  .legend .legend-scale ul {
    margin: 0;
    margin-bottom: 5px;
    padding: 0;
    float: left;
    list-style: none;
  }

  .legend .legend-scale ul li {
    font-size: 80%;
    list-style: none;
    margin-left: 0;
    line-height: 18px;
    margin-bottom: 2px;
  }

  .legend ul.legend-labels li span {
    display: block;
    float: left;
    height: 16px;
    width: 30px;
    margin-right: 5px;
    margin-left: 0;
    border: 1px solid #999;
  }

  .legend a {
    color: #777;
  }
</style>
