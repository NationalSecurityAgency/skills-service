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
  <div>
    <sub-page-header title="Configure Video"/>
    <b-card>
      <div class="row">
        <div class="col">
          <b-form-group label="Video URL:" label-for="videoUrlInput">
            <b-form-input id="videoUrlInput" v-model="videoConf.url"/>
          </b-form-group>
        </div>
        <div class="col-3">
          <b-form-group label="Video Type:" label-for="videoTypeInput">
            <b-form-input id="videoTypeInput" v-model="videoConf.videoType"/>
          </b-form-group>
        </div>
      </div>

      <b-button variant="info" @click="setupPreview">Preview</b-button>

      <div v-if="preview" class="mt-2">
        <skills-video-player :video="{ url: this.videoConf.url, videoType: this.videoConf.videoType }" />
      </div>
    </b-card>
  </div>
</template>

<script>
  import SubPageHeader from '@/components/utils/pages/SubPageHeader';
  import SkillsVideoPlayer from '@/components/video/SkillsVideoPlayer';

  export default {
    name: 'VideoConfigPage',
    components: { SkillsVideoPlayer, SubPageHeader },
    data() {
      return {
        videoConf: {
          url: '',
          videoType: 'video/webm',
        },
        preview: false,
      };
    },
    methods: {
      setupPreview() {
        this.preview = true;
      },
    },
  };
</script>

<style scoped>

</style>
