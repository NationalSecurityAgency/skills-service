<script setup>
import tinycolor from 'tinycolor2';

const props = defineProps({
  color: {
    type: String,
    default: '#4472ba',
  },
})
const shadowColor0 = tinycolor(props.color).darken(10).toString();
const shadowColor1 = tinycolor(props.color).darken(20).toString();


</script>
<template>
  <div class="non-semantic-protector">
    <div
      :style="{ 'background': color }"
      class="category-ribbon">
      <span
        :style="{ 'border-color': `${shadowColor0} ${shadowColor0} ${shadowColor0} transparent` }"
        class="ribbon-shadow-0 before" />
      <span class="category-ribbon-content">
        <span
          :style="{ 'border-color': `${shadowColor1} transparent transparent transparent` }"
          class="ribbon-shadow-1 before" />
        <slot />
        <span
          :style="{ 'border-color': `${shadowColor1} transparent transparent transparent` }"
          class="ribbon-shadow-1 after" />
      </span>
      <span
        :style="{ 'border-color': `${shadowColor0} transparent ${shadowColor0} ${shadowColor0}` }"
        class="ribbon-shadow-0 after" />
    </div>
  </div>
</template>

<style scoped>
.modal-header .non-semantic-protector {
  /* #9808 */
  /* Since the X close button is float right the banner will be on top of it preventing clicking */
  max-width: 95%;
  padding-left: 5%;
}

.non-semantic-protector {
  position: relative;
  z-index: 1;

  .category-ribbon {
    font-size: 1rem;
    color: #ffffff;
  }

  .category-ribbon {
    width: 80%;
    position: relative;
    text-align: center;
    padding: 0.3em 0.5em;
    margin: 0.5em auto 1em;
  }
  .category-ribbon-content {
    font-weight: bold;
  }

  .category-ribbon .ribbon-shadow-0 {
    content: "";
    position: absolute;
    display: block;
    bottom: -0.7em;
    border: 0.8em solid blue;
    z-index: -1;
  }

  .category-ribbon .ribbon-shadow-0.before {
    left: -1.1em;
    border-right-width: 1em;
    border-left-color: transparent;
  }

  .category-ribbon .ribbon-shadow-0.after {
    right: -1.1em;
    border-left-width: 1em;
    border-right-color: transparent;
  }

  .category-ribbon .category-ribbon-content .ribbon-shadow-1.before, .category-ribbon .category-ribbon-content .ribbon-shadow-1.after {
    content: "";
    position: absolute;
    display: block;
    border-style: solid;
    bottom: -0.7em;
  }

  .category-ribbon .category-ribbon-content .ribbon-shadow-1.before {
    left: 0;
    border-width: 0.7em 0 0 0.7em;
  }

  .category-ribbon .category-ribbon-content .ribbon-shadow-1.after {
    right: 0;
    border-width: 0.7em 0.7em 0 0;
  }
}
</style>