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
<script>
  export default {
    name: 'ScrollSkillIntoViewMixin',
    data() {
      return {
        jumpToLastViewed: false,
        lastViewedSkillId: null,
      };
    },
    methods: {
      isScrollFeatureSupported() {
        const opts = this.$store.state.options;
        return opts && Object.keys(opts).length > 0;
      },
      autoScrollToLastViewedSkill() {
        if (this.isScrollFeatureSupported()) {
          setTimeout(() => {
            if (this.jumpToLastViewed) {
              this.$nextTick(() => {
                this.scrollToLastViewedSkill();
              });
            }
          }, 400);
        }
      },
      scrollToLastViewedSkill() {
        if (this.isScrollFeatureSupported() && this.lastViewedSkillId) {
          const elementId = `skillRow-${this.lastViewedSkillId}`;
          const element = document.getElementById(elementId);
          if (element) {
            const opts = this.$store.state.options;
            const shouldUseScrollOffsetStrategy = (opts.autoScrollStrategy === 'top-offset') && opts.scrollTopOffset && opts.scrollTopOffset > 0;

            this.$nextTick(() => {
              if (shouldUseScrollOffsetStrategy) {
                const scrollToElement = document.getElementById(elementId);
                const distanceFromTop = scrollToElement.getBoundingClientRect().top;
                if (this.$store.state.parentFrame) {
                  this.$store.state.parentFrame.emit('do-scroll', distanceFromTop);
                } else {
                  // eslint-disable-next-line
                  console.warn('WARNING: scrollInfo as provided but the code is not running within iframe');
                }
              } else {
                element.scrollIntoView({ behavior: 'smooth' });
              }
              this.$nextTick(() => {
                const idToFocusOn = `skillProgressTitle-${this.lastViewedSkillId}`;
                const elementFocusOn = document.getElementById(idToFocusOn);
                elementFocusOn.focus({ preventScroll: true });
              });
            });
          }
        }
      },
    },
  };
</script>

<style scoped>

</style>
