/*
Copyright 2025 SkillTree

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
<script setup lang="ts">
import {ref, onMounted, watch, computed, shallowRef, onUnmounted} from 'vue';
import * as pdfjsLib from 'pdfjs-dist';
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url';
import 'pdfjs-dist/web/pdf_viewer.css';
import { useDebounceFn } from '@vueuse/core'

const props = defineProps({
  pdfUrl: {
    type: String,
    required: true
  },
  maxWidth: {
    type: Number,
    required: false,
  },
});

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker;

const isInitLoading = ref(true);
const isRendingPage = ref(false);
const pdfDoc = shallowRef({});
const currentPage = ref(1)

const width = ref(0)
const height = ref(0)

onMounted(() => {
  loadPdf();
});

const loadPdf = () => {
  const loadingTask = pdfjsLib.getDocument(props.pdfUrl);
  loadingTask.promise.then((pdf) => {
    pdfDoc.value = pdf;
    renderPage(currentPage.value).then(() => {
      isInitLoading.value = false;
    })
  })
}

const renderWithDebounce = useDebounceFn((pageNum) => {
  renderPage(pageNum)
}, 350)

watch(() => props.maxWidth, () => {
  if(!isInitLoading.value) {
    renderWithDebounce(currentPage.value)
  }
})

const renderPage = (pageNum) => {
  if (isRendingPage.value) {
    return Promise.resolve();
  }
  isRendingPage.value = true;
  const pdfDocLocal = pdfDoc.value;
  return pdfDocLocal.getPage(pageNum).then((page) => {
    const scale = 1;
    let viewport = page.getViewport({scale: scale,});
    if (props.maxWidth && props.maxWidth < viewport.width) {
      const newScale = props.maxWidth / viewport.width;
      viewport = page.getViewport({scale: newScale,});
    }

    // Support HiDPI-screens.
    let outputScale = window.devicePixelRatio || 1;

    const canvas = document.getElementById('pdfCanvasId');
    const context = canvas.getContext('2d');

    canvas.width = Math.floor(viewport.width * outputScale);
    canvas.height = Math.floor(viewport.height * outputScale);
    canvas.style.width = Math.floor(viewport.width) + "px";
    canvas.style.height = Math.floor(viewport.height) + "px";

    width.value = viewport.width
    height.value = viewport.height

    const transform = outputScale !== 1
        ? [outputScale, 0, 0, outputScale, 0, 0]
        : null;

    const renderContext = {
      canvasContext: context,
      transform: transform,
      viewport: viewport
    };

    page.render(renderContext).promise.then(function () {
      return page.getTextContent();
    }).then(function (textContent) {
      // const textLayerDiv = document.getElementById("text-layer")
      const textLayerDiv = document.getElementById("text-layer")

      // Clear previous text layer content
      textLayerDiv.innerHTML = '';

      const textLayer = new pdfjsLib.TextLayer({
        textContentSource: textContent,
        viewport: viewport,
        container: textLayerDiv,
      });

      textLayer.render().then(() => {
        currentPage.value = pageNum
        isRendingPage.value = false
        page.cleanup();
      })
    });
  })
}

const totalPages = computed(() => pdfDoc.value?.numPages || 0)

const prevPage = () => {
  if (!isRendingPage.value) {
    renderPage(currentPage.value - 1)
  }
}
const nextPage = () => {
  if (!isRendingPage.value) {
    renderPage(currentPage.value + 1)
  }
}
const progressPercent = computed(() => (currentPage.value / totalPages.value) * 100)

const resizeSmaller = () => resize(-50)
const resizeBigger = () => resize(50)
const resize = (resizeWidth) => {
  console.log(resizeWidth)
}
</script>

<template>
  <div>
    <div>
      <skills-spinner :is-loading="isInitLoading" v-if="isInitLoading"/>
      <div class="flex justify-center">
<!--        <i class="fas fa-expand-alt fa-rotate-90 handle border border-surface-500 dark:border-surface-300 p-1 text-primary bg-primary-contrast rounded-border"-->
<!--           data-cy="videoResizeHandle"-->
<!--           role="button"-->
<!--           aria-label="Resize video dimensions control. Press right or left to resize the video player."-->
<!--           @keyup.right="resizeBigger"-->
<!--           @keyup.left="resizeSmaller"-->
<!--           tabindex="0"></i>-->
<!--        <div class="absolute right-0 top-1/2 transform translate-x-1/2 -translate-y-1/2">-->
<!--          <i class="fas fa-expand-alt fa-rotate-90 handle border border-surface-500 dark:border-surface-300 p-1 text-primary bg-primary-contrast rounded cursor-pointer hover:bg-surface-200 dark:hover:bg-surface-600"-->
<!--             data-cy="videoResizeHandle"-->
<!--             @click="resizeBigger"-->
<!--             @keyup.enter="resizeBigger"-->
<!--             tabindex="0"-->
<!--             aria-label="Resize slides">-->
<!--          </i>-->
<!--        </div>-->
        <div class="border-1 rounded ml-2 relative" :style="`height: ${height+5}px; width: ${width+5}px;};`">
          <canvas id="pdfCanvasId" class="z-0 absolute top-0 left-0"></canvas>
          <div id="text-layer" class="textLayer absolute top-0 left-0 w-full h-full z-50" style="--total-scale-factor: 1"></div>
        </div>
      </div>
    </div>
    <div v-if="!isInitLoading" class="flex justify-center p-2">
      <div class="flex gap-2 items-center">
        <SkillsButton
            aria-label="Previous Slide"
            icon="fa-solid fa-circle-chevron-left"
            @click="prevPage"
            :disabled="currentPage <= 1"/>
        <div>
          <div>Slide {{ currentPage }} of {{ totalPages }}</div>
          <ProgressBar :value="progressPercent" :show-value="false" style="height: 5px"></ProgressBar>
        </div>
        <SkillsButton
            aria-label="Next Slide"
            icon="fa-solid fa-circle-chevron-right"
            @click="nextPage"
            :disabled="currentPage >= totalPages"/>
      </div>
    </div>
  </div>
</template>

<style scoped>

</style>