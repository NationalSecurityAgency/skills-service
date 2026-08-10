/*
 * Copyright 2026 SkillTree
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
import videojs from 'video.js';

// Get the base Component and Button classes from Video.js
const Button = videojs.getComponent('Button');

class ResizeButton extends Button {
    constructor(player, options) {
        super(player, options);
        // Add a custom CSS class for styling/icon placement
        this.addClass('vjs-resize-button fas fa-expand-alt fa-rotate-90');
        this.setAttribute('id', 'resizeHandle')
        // Set screen reader text and tooltip
        this.controlText('Drag to resize');

        this.on('mousedown', this.handleMouseDown);
    }

    handleMouseDown(e) {
        e.preventDefault()
        this.on('mousemove', this.resize)
        this.on('mouseup', this.stopResize)
    }

    resize(e) {
        console.log('resizing...')
        console.log(this.player().currentWidth())
        console.log(this.player().currentHeight())
        // isResizing.value = true
        // const element = getResizableElement();
        // const clientRect = element.getBoundingClientRect()
        console.log(e);
        this.player().width(this.player().currentWidth() + e.movementX)
        // element.style.width = e.pageX - clientRect.left + 'px'
        // updateResizableInfo()
    }

    stopResize() {
        console.log('resize stopped')
        this.off('mousemove', this.resize)
        // isResizing.value = false
        // if (playerWidth.value && playerHeight.value) {
        //     announcer.polite(`Resized the video player to ${playerWidth.value} x ${playerHeight.value}`)
        // }
    }

    // Triggered when the user clicks the button
    handleClick() {
        // const player = this.player();
        // const currentSrc = player.currentSrc();
        // const filenameFromSrc = currentSrc.substring(currentSrc.lastIndexOf('/') + 1);
        // // Use custom download URL if provided in options, otherwise fallback to current src
        // const src = this.options().downloadUrl || currentSrc;
        //
        // if (!src) {
        //     console.warn('No video source available for download.');
        //     return;
        // }
        //
        // // Trigger standard browser download
        // const link = document.createElement('a');
        // link.href = src;
        // link.download = filenameFromSrc || 'video.mp4';
        // link.target = '_blank';
        // document.body.appendChild(link);
        // link.click();
        // document.body.removeChild(link);
    }
}

// Register the custom component with Video.js
videojs.registerComponent('ResizeButton', ResizeButton);

// Register the plugin wrapper
function registerDownloadPlugin() {
    videojs.registerPlugin('resizeButton', function (options = {}) {
        this.ready(() => {
            // Add the ResizeButton to the controlBar if it isn't already there
            if (!this.controlBar.childNameIndex_['ResizeButton']) {
                this.controlBar.addChild('ResizeButton', options);
            }
        });
    });
}

export default registerDownloadPlugin;