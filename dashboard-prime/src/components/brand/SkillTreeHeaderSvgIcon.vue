/*
Copyright 2024 SkillTree

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
<script setup>
import { computed } from 'vue'
import { useThemesHelper } from '@/components/header/UseThemesHelper.js'

const themeHelper = useThemesHelper()
const st4ColorClass = computed(() => themeHelper.isDarkTheme ? 'st4-dark': 'st4')
</script>

<template>
  <div>

       <svg version="1.1" xmlns:x="&ns_extend;" xmlns:i="&ns_ai;" xmlns:graph="&ns_graphs;"
            xmlns="http://www.w3.org/2000/svg" xmlns:xlink="http://www.w3.org/1999/xlink" x="0px" y="0px"
            viewBox="0 0 190 45" aria-label="SkillTree Logo"
            style="enable-background:new 0 0 716 384;" xml:space="preserve" height="3rem">
      <g>
	<g>
		<g>
			<path class="st0" d="M1.84,9.36l9.42-6.85l9.42,6.85c0.21,0.15,0.44,0.22,0.68,0.22c0.36,0,0.71-0.17,0.94-0.48
				c0.38-0.52,0.26-1.24-0.26-1.62L11.95,0.14c-0.41-0.3-0.96-0.3-1.36,0L0.48,7.48c-0.52,0.38-0.63,1.1-0.26,1.62
				C0.6,9.62,1.32,9.74,1.84,9.36z"/>
      <path class="st1" d="M22.05,14.01L11.95,6.66c-0.41-0.3-0.96-0.3-1.36,0L0.48,14.01c-0.52,0.38-0.63,1.1-0.26,1.62
				c0.38,0.52,1.1,0.63,1.62,0.26l9.42-6.85l9.42,6.85c0.21,0.15,0.44,0.22,0.68,0.22c0.36,0,0.71-0.17,0.94-0.48
				C22.69,15.11,22.57,14.38,22.05,14.01z"/>
      <path class="st2" d="M22.05,20.53l-10.11-7.34c-0.41-0.3-0.96-0.3-1.36,0L0.48,20.53c-0.52,0.38-0.63,1.1-0.26,1.62
				c0.38,0.52,1.1,0.63,1.62,0.26l9.42-6.85l9.42,6.85c0.21,0.15,0.44,0.22,0.68,0.22c0.36,0,0.71-0.17,0.94-0.48
				C22.69,21.63,22.57,20.91,22.05,20.53z"/>
      <path class="st3" d="M22.05,27.05l-10.11-7.34c-0.41-0.3-0.96-0.3-1.36,0L0.48,27.05c-0.52,0.38-0.63,1.1-0.26,1.62
				c0.38,0.52,1.1,0.63,1.62,0.26l9.42-6.85l9.42,6.85c0.21,0.15,0.44,0.22,0.68,0.22c0.36,0,0.71-0.17,0.94-0.48
				C22.69,28.15,22.57,27.43,22.05,27.05z"/>
      <g>
				<path :class="st4ColorClass" d="M12.39,43.79c0,0.64-0.48,1.13-1.14,1.13c-0.66,0-1.17-0.49-1.17-1.13V29.46l-8.25,5.99
					c-0.52,0.38-1.24,0.26-1.62-0.26c-0.38-0.52-0.26-1.24,0.26-1.62l10.11-7.34c0.41-0.3,0.96-0.3,1.36,0l10.11,7.34
					c0.52,0.38,0.63,1.1,0.26,1.62c-0.23,0.31-0.58,0.48-0.94,0.48c-0.24,0-0.47-0.07-0.68-0.22l-8.26-6L12.39,43.79z"/>
			</g>
		</g>
	</g>
        <g>
		<path :class="st4ColorClass" d="M40.56,35.07c-0.28-0.2-0.56-0.6-0.56-1.13c0-0.72,0.6-1.33,1.37-1.33c0.28,0,0.56,0.08,0.76,0.24
			c2.13,1.45,4.34,2.21,6.59,2.21c2.49,0,4.3-1.29,4.3-3.3v-0.08c0-2.09-2.45-2.89-5.18-3.66c-3.26-0.92-6.87-2.05-6.87-5.87v-0.08
			c0-3.58,2.97-5.95,7.07-5.95c2.21,0,4.62,0.68,6.63,1.77c0.4,0.24,0.76,0.68,0.76,1.29c0,0.72-0.6,1.33-1.37,1.33
			c-0.28,0-0.52-0.08-0.68-0.16c-1.77-1-3.66-1.61-5.43-1.61c-2.45,0-4.02,1.29-4.02,3.01v0.08c0,1.97,2.57,2.73,5.34,3.58
			c3.22,0.96,6.67,2.21,6.67,5.95v0.08c0,3.94-3.25,6.23-7.39,6.23C45.83,37.68,42.81,36.67,40.56,35.07z"/>
          <path :class="st4ColorClass" d="M61.7,9.31c0-0.84,0.68-1.57,1.53-1.57c0.89,0,1.57,0.72,1.57,1.57v19.05L76.05,16.9
			c0.36-0.4,0.72-0.6,1.21-0.6c0.84,0,1.41,0.68,1.41,1.45c0,0.48-0.2,0.8-0.6,1.21l-6.79,6.67l7.43,9.36
			c0.28,0.36,0.4,0.64,0.4,1.04c0,0.84-0.64,1.45-1.49,1.45c-0.56,0-0.92-0.2-1.33-0.72l-7.19-9.08l-4.3,4.26v3.98
			c0,0.88-0.64,1.57-1.53,1.57c-0.89,0-1.57-0.68-1.57-1.57V9.31z"/>
          <path :class="st4ColorClass" d="M84.53,9.99c0-1,0.8-1.65,1.81-1.65c1.04,0,1.85,0.64,1.85,1.65v0.52c0,0.96-0.8,1.65-1.85,1.65
			c-1,0-1.81-0.68-1.81-1.65V9.99z M84.81,17.87c0-0.84,0.68-1.57,1.53-1.57c0.89,0,1.57,0.68,1.57,1.57v18.04
			c0,0.88-0.64,1.57-1.53,1.57c-0.89,0-1.57-0.68-1.57-1.57V17.87z"/>
          <path :class="st4ColorClass" d="M95.39,9.31c0-0.84,0.68-1.57,1.53-1.57c0.89,0,1.57,0.72,1.57,1.57v26.6c0,0.88-0.64,1.57-1.53,1.57
			c-0.89,0-1.57-0.68-1.57-1.57V9.31z"/>
          <path :class="st4ColorClass" d="M105.96,9.31c0-0.84,0.68-1.57,1.53-1.57c0.89,0,1.57,0.72,1.57,1.57v26.6c0,0.88-0.64,1.57-1.53,1.57
			c-0.89,0-1.57-0.68-1.57-1.57V9.31z"/>
          <path :class="st4ColorClass" d="M117.33,31.77V19.23h-1.69c-0.72,0-1.37-0.64-1.37-1.37c0-0.76,0.64-1.37,1.37-1.37h1.69v-4.9
			c0-0.84,0.64-1.57,1.53-1.57c0.84,0,1.57,0.72,1.57,1.57v4.9h5.39c0.76,0,1.41,0.64,1.41,1.37c0,0.76-0.64,1.37-1.41,1.37h-5.39
			v12.14c0,2.53,1.41,3.46,3.5,3.46c1.08,0,1.65-0.28,1.89-0.28c0.72,0,1.33,0.6,1.33,1.33c0,0.56-0.36,1.01-0.89,1.21
			c-0.88,0.36-1.85,0.56-3.05,0.56C119.86,37.64,117.33,35.99,117.33,31.77z"/>
          <path :class="st4ColorClass" d="M132.48,17.87c0-0.84,0.68-1.57,1.53-1.57c0.89,0,1.57,0.68,1.57,1.57v3.9c1.53-3.46,4.54-5.55,7.15-5.55
			c0.92,0,1.53,0.68,1.53,1.57c0,0.84-0.56,1.45-1.37,1.57c-4.02,0.48-7.31,3.5-7.31,9.48v7.07c0,0.84-0.64,1.57-1.53,1.57
			c-0.89,0-1.57-0.68-1.57-1.57V17.87z"/>
          <path :class="st4ColorClass" d="M156.96,37.76c-5.71,0-10.37-4.38-10.37-10.81v-0.08c0-5.99,4.22-10.81,9.97-10.81
			c6.15,0,9.65,5.02,9.65,10.53c0,0.84-0.68,1.45-1.45,1.45h-15.03c0.44,4.5,3.62,7.03,7.31,7.03c2.57,0,4.46-1,5.99-2.41
			c0.24-0.2,0.52-0.36,0.89-0.36c0.76,0,1.37,0.6,1.37,1.33c0,0.36-0.16,0.72-0.48,1C162.83,36.51,160.5,37.76,156.96,37.76z
			 M163.11,25.74c-0.32-3.78-2.49-7.07-6.63-7.07c-3.62,0-6.35,3.01-6.75,7.07H163.11z"/>
          <path :class="st4ColorClass" d="M180.76,37.76c-5.71,0-10.37-4.38-10.37-10.81v-0.08c0-5.99,4.22-10.81,9.97-10.81
			c6.15,0,9.65,5.02,9.65,10.53c0,0.84-0.68,1.45-1.45,1.45h-15.03c0.44,4.5,3.62,7.03,7.31,7.03c2.57,0,4.46-1,5.99-2.41
			c0.24-0.2,0.52-0.36,0.89-0.36c0.76,0,1.37,0.6,1.37,1.33c0,0.36-0.16,0.72-0.48,1C186.62,36.51,184.29,37.76,180.76,37.76z
			 M186.9,25.74c-0.32-3.78-2.49-7.07-6.63-7.07c-3.62,0-6.35,3.01-6.75,7.07H186.9z"/>
	</g>
</g>
</svg>

  </div>
</template>

<style scoped>
.st0{fill:#E76F50;}
.st1{fill:#F3A161;}
.st2{fill:#E8C269;}
.st3{fill:#2B9C8E;}
.st4{fill:#254653;}
.st4-dark{fill: #f9fafb;}
</style>