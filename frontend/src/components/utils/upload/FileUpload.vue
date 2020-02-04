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
        console.log('filesChange event called');
        const formData = new FormData();

        console.log(`${fieldName} - ${fileList.length}`);
        console.log(fileList);
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
