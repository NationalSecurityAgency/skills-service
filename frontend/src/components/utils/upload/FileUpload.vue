<template>
  <div class="uploadContainer">
    <form enctype="multipart/form-data" novalidate>
      <h1 slot="title"></h1>
      <div class="dropbox">
        <input type="file"
               :name="name"
               :disabled="isSaving"
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
  import ToastHelper from '../ToastHelper';
  import FileUploadService from './FileUploadService';

  const STATUS_INITIAL = 0;
  const STATUS_SAVING = 1;
  const STATUS_SUCCESS = 2;
  const STATUS_FAILED = 3;

  const DEFAULT_STATUS_MSG = 'Drag your file here to upload or click to browse';

  export default {
    name: 'FileUpload',
    props: ['url', 'name', 'accept', 'validateImages', 'imageWidth', 'imageHeight'],
    data() {
      return {
        uploadError: null,
        currentStatus: null,
        uploadedFileName: '',
        statusMsg: DEFAULT_STATUS_MSG,
      };
    },
    computed: {
      isInitial() {
        return this.currentStatus === STATUS_INITIAL;
      },
      isSaving() {
        return this.currentStatus === STATUS_SAVING;
      },
      isSuccess() {
        return this.currentStatus === STATUS_SUCCESS;
      },
      isFailed() {
        return this.currentStatus === STATUS_FAILED;
      },
    },
    mounted() {
      this.reset();
    },
    methods: {
      reset() {
        this.currentStatus = STATUS_INITIAL;
        this.uploadError = null;
        this.statusMsg = DEFAULT_STATUS_MSG;
      },
      setTemporaryStatusMsg(status) {
        this.statusMsg = status;
        setTimeout(() => {
          this.statusMsg = DEFAULT_STATUS_MSG;
        }, 7000);
      },
      save(formData) {
        this.currentStatus = STATUS_SAVING;
        this.statusMsg = 'Uploading...';
        const self = this;

        FileUploadService.upload(this.url, formData, (response) => {
          self.currentStatus = STATUS_SUCCESS;
          self.statusMsg = DEFAULT_STATUS_MSG;
          self.$emit('upload-success', response.data);
          self.$toast.open(ToastHelper.defaultConf('File successfully uploaded'));
          self.reset();
        }, (err) => {
          self.uploadError = err.response;
          self.currentStatus = STATUS_FAILED;

          self.setTemporaryStatusMsg('<span class="upload-failure">Upload Failed</span>');

          let msg = this.uploadError.statusText;
          if (this.uploadError.data && this.uploadError.data.message) {
            msg = this.uploadError.data.message;
          }
          throw msg;
        });
      },
      filesChange(fieldName, fileList) {
        const formData = new FormData();

        if (!fileList.length) return;

        Array.from(Array(fileList.length).keys())
          .map((x) => {
            formData.append(fieldName, fileList[x], fileList[x].name);
            return x;
        });

        const saveAction = (() => this.save(formData));

        if (this.validateImages && this.imageWidth && this.imageHeight) {
          const file = formData.get(this.name);
          const isImageType = file.type.startsWith('image/');

          if (file && isImageType) {
            const image = new Image();
            image.src = window.URL.createObjectURL(file);
            image.onload = () => {
              const width = image.naturalWidth;
              const height = image.naturalHeight;
              window.URL.revokeObjectURL(image.src);

              if (width === this.imageWidth && height === this.imageHeight) {
                saveAction();
              } else {
                const errorMsg = `Invalid image dimensions, ${file.name} must be  ${this.imageHeight} x ${this.imageWidth}`;
                this.setTemporaryStatusMsg(`<span class="upload-failure">${errorMsg}</span>`);
                throw new Error(errorMsg);
              }
            };
          }
        } else {
          saveAction();
        }
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
