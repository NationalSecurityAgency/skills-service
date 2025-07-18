<script setup lang="ts">
import {ref, onMounted, watch, computed, shallowRef} from 'vue';
import * as pdfjsLib from 'pdfjs-dist';
import pdfWorker from 'pdfjs-dist/build/pdf.worker.min.mjs?url';

const props = defineProps({
  pdfUrl: {
    type: String,
    required: true
  },
});

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker;

const isInitLoading = ref(true);
const isRendingPage = ref(true);
const pdfDoc = shallowRef({});
const currentPage = ref(1)

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

const renderPage = (pageNum) => {
  const pdfDocLocal = pdfDoc.value;
  return pdfDocLocal.getPage(pageNum).then((page) => {
    const scale = 1;
    const viewport = page.getViewport({scale: scale,});
    // Support HiDPI-screens.
    const outputScale = window.devicePixelRatio || 1;

    const canvas = document.getElementById('pdfCanvasId');
    const context = canvas.getContext('2d');

    canvas.width = Math.floor(viewport.width * outputScale);
    canvas.height = Math.floor(viewport.height * outputScale);
    canvas.style.width = Math.floor(viewport.width) + "px";
    canvas.style.height = Math.floor(viewport.height) + "px";

    const transform = outputScale !== 1
        ? [outputScale, 0, 0, outputScale, 0, 0]
        : null;

    const renderContext = {
      canvasContext: context,
      transform: transform,
      viewport: viewport
    };
    page.render(renderContext);
    currentPage.value = pageNum
    isRendingPage.value = false
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
</script>

<template>
  <div>
    <div>
      <skills-spinner :is-loading="isInitLoading" v-if="isInitLoading"/>
      <div class="border-2 rounded flex justify-center">
        <canvas id="pdfCanvasId"></canvas>
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