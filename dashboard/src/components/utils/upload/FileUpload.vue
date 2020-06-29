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
  <div class="uploadContainer">
    <form enctype="multipart/form-data" novalidate>
      <h1 slot="title"></h1>
      <div class="dropbox">
        <input type="file"
               :name="name"
               :disabled="disableInput"
               @change="filesChange($event.target.name, $event.target.files)"
               :accept="accept"
               class="input-file"/>
        <p v-html="statusMsg">
        </p>
      </div>
    </form>
  </div>
</template>

<script>
  const DEFAULT_STATUS_MSG = 'Drag your file here to upload or click to browse';

  export default {
    $_veeValidate: {
      value() {
        return this.getFormData();
      },
      name() {
        return this.name;
      },
    },
    name: 'FileUpload',
    props: ['name', 'accept'],
    data() {
      return {
        uploadError: null,
        currentStatus: null,
        uploadedFileName: '',
        statusMsg: DEFAULT_STATUS_MSG,
        formData: null,
        label: null,
        disableInput: false,
      };
    },
    methods: {
      getFormData() {
        return this.formData;
      },
      filesChange(fieldName, fileList) {
        const formData = new FormData();

        if (!fileList.length) return;

        Array.from(Array(fileList.length).keys())
          .map((x) => {
            formData.append(fieldName, fileList[x], fileList[x].name);
            return x;
          });

        this.formData = formData;

        this.$emit('file-selected', { fieldname: this.name, form: this.formData });
      },
    },
  };
</script>

<style>
  .dropbox {
    outline: 2px dashed white;
    outline-offset: -10px;
    background: #3273dc;
    color: white;
    min-height: 130px;
    position: relative;
    cursor: pointer;
  }

  .input-file {
    opacity: 0;
    width: 100%;
    height: 130px;
    position: absolute;
    cursor: pointer;
  }

  .dropbox:hover {
    background: lightgray;
    color: white;
  }

  .dropbox p {
    font-size: 1.2em;
    text-align: center;
    padding: 20px 20px;
  }

  .upload-failure {
    color: red;
    font-weight: bold;
  }
</style>
