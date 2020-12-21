/*
Copyright 2020 SkillTree

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
*/
<template>
    <div class="icon-row" :key="source.id">
      <div class="icon-item" v-for="item in iconRows" :key="item.cssClass">
        <a
          href="#"
          @click.stop.prevent="handleClick(item.name, item.cssClass)"
          :class="`icon-${item.name}`"
          :data-cy="`${item.cssClass}-link`"
          :aria-label="`select icon ${item.name}`">
          <span class="icon is-large">
            <i :class="item.cssClass"></i>
          </span>
        </a><br/>
        <span class="iconName">{{ item.name }}</span>
      </div>
    </div>
</template>

<script>
  export default {
    name: 'IconRow',
    props: {
      source: {
        type: Object,
        default() {
          return [];
        },
      },
    },
    data() {
      return {
        iconRows: this.source.row,
      };
    },
    methods: {
      handleClick(name, cssClass) {
        // hacky but can't pass an event from grand-child to grand-parent otherwise
        this.$parent.$parent.$emit('icon-selected', { name, cssClass });
      },
    },
  };
</script>

<style scoped>
.icon-row {
  display: flex;
  flex-direction: row;
  justify-content: space-evenly;
  padding-top: 12px;
  padding-bottom: 12px;
}

.icon-item {
  border-radius: 3px;
  color: inherit;
  flex: auto;
  text-align: center;
}

.icon-item i {
  box-sizing: content-box;
  text-align: center;
  border-radius: 3px;
  font-size: 3rem;
  width: 48px;
  height: 48px;
  display: inline-block;
}

.tab-content div {
  width: 100%;
}

.delete-icon {
  left: 0;
  visibility: hidden;
}

.is-tiny {
  height: .5rem;
  width: .5rem;
  font-size: .5rem;
  padding-right: .5rem;
}

.icon-item:hover .delete-icon {
  z-index: 1000;
  display: inline-block;
  visibility: visible;
}
</style>
