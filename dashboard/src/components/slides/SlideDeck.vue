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
import {useDebounceFn} from '@vueuse/core'
import {useSkillsAnnouncer} from "@/common-components/utilities/UseSkillsAnnouncer.js";

const props = defineProps({
  slidesId: {
    type: String,
    required: true
  },
  pdfUrl: {
    type: String,
    required: true
  },
  defaultWidth: {
    type: Number,
    required: false,
  },
  maxWidth: {
    type: Number,
    required: false,
  },
});
const emit = defineEmits(['on-resize'])

pdfjsLib.GlobalWorkerOptions.workerSrc = pdfWorker;

const announcer = useSkillsAnnouncer()

const isInitLoading = ref(true);
const isRendingPage = ref(false);
const pdfDoc = shallowRef({});
const currentPage = ref(1)

const width = ref(0)
const height = ref(0)

const isFullscreen = ref(false);
const containerRef = ref(null);

const toggleFullscreen = () => {
  if (!document.fullscreenElement) {
    containerRef.value?.requestFullscreen?.().then(() => {
      isFullscreen.value = true;
      renderPage(currentPage.value)
    });
  } else {
    document.exitFullscreen().then(() => {
      isFullscreen.value = false;
      renderPage(currentPage.value)
    })
  }
}

// Handle fullscreen change events
const handleFullscreenChange = () => {
  isFullscreen.value = !!document.fullscreenElement;
};

onMounted(() => {
  loadPdf().then(() => {
    createResizeSupport()
  })
  document.addEventListener('fullscreenchange', handleFullscreenChange);
  document.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
  document.removeEventListener('fullscreenchange', handleFullscreenChange);
  document.removeEventListener('keydown', handleKeyDown);
});

const handleKeyDown = (e) => {
  if (!isFullscreen.value) return;

  switch (e.key) {
    case 'ArrowRight':
    case ' ':
    case 'Enter':
      e.preventDefault();
      nextPage();
      break;
    case 'ArrowLeft':
    case 'Backspace':
      e.preventDefault();
      prevPage();
      break;
    case 'Escape':
      if (document.fullscreenElement) {
        document.exitFullscreen();
      }
      break;
  }
};

const loadPdf = () => {
  const loadingTask = pdfjsLib.getDocument(props.pdfUrl);
  return loadingTask.promise.then((pdf) => {
    pdfDoc.value = pdf;
    return renderPage(currentPage.value).then(() => {
      isInitLoading.value = false;
    })
  })
}

const renderWithDebounce = useDebounceFn((pageNum, newWidth) => {
  renderPage(pageNum, newWidth)
}, 350)

watch(() => props.maxWidth, () => {
  if (!isInitLoading.value) {
    renderWithDebounce(currentPage.value, null)
  }
})

const renderPage = (pageNum, requestedWidth = null) => {
  const uuid = Math.random().toString(36).substring(2, 9);
  if (isRendingPage.value || pageNum < 1 || pageNum > totalPages.value) {
    return Promise.resolve();
  }
  isRendingPage.value = true;
  const pdfDocLocal = pdfDoc.value;
  return pdfDocLocal.getPage(pageNum).then((page) => {
    const scale = 1;
    let viewport = page.getViewport({scale: scale,});
    if (isFullscreen.value) {
      const newScale = window.innerWidth / viewport.width;
      viewport = page.getViewport({scale: newScale,});
    } else if (props.maxWidth && props.maxWidth < viewport.width) {
      const newScale = props.maxWidth / viewport.width;
      viewport = page.getViewport({scale: newScale,});
    } else if (requestedWidth != null) {
      const newScale = requestedWidth / viewport.width;
      viewport = page.getViewport({scale: newScale,});
    } else if (props.defaultWidth != null) {
      const widthToUse = Math.min(props.defaultWidth, props.maxWidth || Number.MAX_SAFE_INTEGER)
      const newScale = widthToUse / viewport.width;
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
        announcer.polite(`Rendered pdf page ${pageNum} of ${pdfDoc.value.numPages}`)
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

const isResizing = ref(false)

const getResizableElement = () => {
  const resizableDiv = `#${props.slidesId}Container`
  return document.querySelector(resizableDiv)
}
const createResizeSupport = () => {
  function makeResizableDiv() {
    const handle = document.querySelectorAll(`#${props.slidesId}ResizeHandle`)[0]

    handle.addEventListener('mousedown', function (e) {
      e.preventDefault()
      window.addEventListener('mousemove', resize)
      window.addEventListener('mouseup', stopResize)
    })

    let latestWidth = 0;

    function resize(e) {
      isResizing.value = true
      const element = getResizableElement();
      const clientRect = element.getBoundingClientRect()
      latestWidth = e.pageX - clientRect.left
      renderPage(currentPage.value, latestWidth)
    }

    function stopResize() {
      window.removeEventListener('mousemove', resize)
      isResizing.value = false
      if (latestWidth > 0) {
        emit('on-resize', latestWidth)
        announcer.polite(`Resized the slides to ${latestWidth} width`)
      }
    }
  }

  makeResizableDiv()
}

const resizeBigger = () => {
  const newWidth = width.value + 10
  renderPage(currentPage.value, newWidth)
  emit('on-resize', newWidth)
  announcer.polite(`Resized the slides to ${newWidth} width`)
}
const resizeSmaller = () => {
  const newWidth = width.value - 10
  renderPage(currentPage.value, newWidth)
  emit('on-resize', newWidth)
  announcer.polite(`Resized the slides to ${newWidth} width`)
}

const slidesContainerStyle = computed(() => {
  if (!isFullscreen.value) {
    return {
      height: `${height.value + 5}px`,
      width: `${width.value + 5}px`,
    }
  }
  return {}
})

</script>

<template>
  <div>
    <div>
      <skills-spinner :is-loading="isInitLoading" v-if="isInitLoading"/>
      <div class="flex justify-center">
        <div class="flex flex-col">
          <div
              class="flex justify-end p-2 bg-surface-100 dark:bg-surface-700 border-l-1 border-r-1 border-t-1 rounded-t gap-2">
            <SkillsButton
                icon="fa-solid fa-expand"
                size="small"
                v-if="!isFullscreen"
                :id="`${slidesId}FullscreenBtn`"
                class="shadow-md"
                aria-label="Enter fullscreen mode"
                @click="toggleFullscreen"
            />
            <button
                v-if="isFullscreen"
                :id="`${slidesId}ExitFullscreenBtn`"
                class="p-1 rounded shadow-md bg-surface-100 dark:bg-surface-700 text-primary hover:bg-surface-200 dark:hover:bg-surface-600"
                aria-label="Exit fullscreen mode"
                @click="toggleFullscreen"
            >
              <i class="fas fa-compress"></i>
            </button>
          </div>
          <div ref="containerRef" :id="`${slidesId}Container`"
               class="border-r-1 border-l-1 relative hover-resize-container"
               :class="{'presentation-mode bg-surface-700': isFullscreen}"
               :style="slidesContainerStyle">
            <canvas id="pdfCanvasId" class="z-0 absolute top-0 left-0"></canvas>
            <div id="text-layer" class="textLayer absolute top-0 left-0 w-full h-full z-40"
                 style="--total-scale-factor: 1"></div>

            <button
                v-if="!isFullscreen"
                :id="`${slidesId}ResizeHandle`"
                class="resize-handle absolute bottom-2 right-2 px-1 rounded shadow-md z-50 bg-surface-100 dark:bg-surface-700 text-primary cursor-ew-resize hover:bg-surface-200 dark:hover:bg-surface-600"
                aria-label="Resize slides dimensions. Press right or left to resize."
                @keyup.right="resizeBigger"
                @keyup.left="resizeSmaller"
            >
              <i class="fas fa-expand-alt fa-rotate-90"></i>
            </button>
          </div>
          <div v-if="!isInitLoading"
               class="flex items-center justify-between bg-surface-100 dark:bg-surface-700 border-l-1 border-r-1 border-b-1 rounded-b p-2">
            <div class="flex-1"></div> <!-- Spacer for left side -->
            <div class="flex gap-2 items-center">
              <SkillsButton
                  aria-label="Previous Slide"
                  icon="fa-solid fa-circle-chevron-left"
                  class="shadow-md"
                  @click="prevPage"
                  :disabled="currentPage <= 1"/>
              <div>
                <div>Slide {{ currentPage }} of {{ totalPages }}</div>
                <ProgressBar :value="progressPercent" :show-value="false" style="height: 5px"></ProgressBar>
              </div>
              <SkillsButton
                  aria-label="Next Slide"
                  icon="fa-solid fa-circle-chevron-right"
                  class="shadow-md"
                  @click="nextPage"
                  :disabled="currentPage >= totalPages"/>
            </div>
            <div v-if="!isFullscreen" class="flex-1 flex justify-end">
              <SkillsButton
                  icon="fa-solid fa-download"
                  class="shadow-md"
                  size="small"/>
            </div>
          </div>
        </div>
      </div>


    </div>
  </div>
</template>

<style scoped>
.resize-handle {
  opacity: 0;
  transition: opacity 0.2s ease-in-out;
}

.hover-resize-container:hover .resize-handle {
  opacity: 1;
}
.resize-handle:focus {
  opacity: 1;
}

.presentation-mode {
  max-width: 100vw;
  max-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
}

.presentation-mode #pdfCanvasId {
  max-width: 100%;
  max-height: 100vh;
  object-fit: contain;
}
</style>