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

class DownloadButton extends Button {
    constructor(player, options) {
        super(player, options);
        // Add a custom CSS class for styling/icon placement
        this.addClass('vjs-download-button');
        // Set screen reader text and tooltip
        this.controlText('Download Video');
    }

    // Triggered when the user clicks the button
    handleClick() {
        const player = this.player();
        const currentSrc = player.currentSrc();
        const filenameFromSrc = currentSrc.substring(currentSrc.lastIndexOf('/') + 1);
        // Use custom download URL if provided in options, otherwise fallback to current src
        const src = this.options().downloadUrl || currentSrc;

        if (!src) {
            console.warn('No video source available for download.');
            return;
        }

        // Trigger standard browser download
        const link = document.createElement('a');
        link.href = src;
        link.download = filenameFromSrc || 'video.mp4';
        link.target = '_blank';
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}

// Register the custom component with Video.js
videojs.registerComponent('DownloadButton', DownloadButton);

// Register the plugin wrapper
function registerDownloadPlugin() {
    videojs.registerPlugin('downloadButton', function (options = {}) {
        this.ready(() => {
            // Add the DownloadButton to the controlBar if it isn't already there
            if (!this.controlBar.childNameIndex_['DownloadButton']) {
                this.controlBar.addChild('DownloadButton', options);
            }
        });
    });
}

export default registerDownloadPlugin;