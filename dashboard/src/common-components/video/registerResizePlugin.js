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

const Button = videojs.getComponent('Button');

class ResizeButton extends Button {
    constructor(player, options) {
        super(player, options);
        this.controlText('Click and Drag to Resize');
        this.addClass('vjs-resize-button fas fa-expand-alt fa-rotate-90');
        this.isDragging = false;
        this.startX = 0;
        this.startWidth = 0;
    }

    handleClick(event) {
        // Prevent default actions
        event.preventDefault();
    }

    createEl() {
        const el = super.createEl();

        // Listen for mousedown to initiate dragging
        el.addEventListener('mousedown', this.onMouseDown.bind(this));

        el.addEventListener('keydown', this.onKeyDown.bind(this));
        el.addEventListener('keyup', this.onKeyUp.bind(this));

        return el;
    }

    onMouseDown(e) {
        e.preventDefault();
        this.isDragging = true;
        this.startX = e.clientX;

        // Get initial width of the container
        const container = this.player_.el().parentElement;
        this.startWidth = container.offsetWidth;

        // Bind global move and up handlers
        this.boundOnMouseMove = this.onMouseMove.bind(this);
        this.boundOnMouseUp = this.onMouseUp.bind(this);

        window.addEventListener('mousemove', this.boundOnMouseMove);
        window.addEventListener('mouseup', this.boundOnMouseUp);

        this.player_.trigger('resizeStart');
    }

    onMouseMove(e) {
        if (!this.isDragging) return;

        // Calculate distance moved from starting point
        const deltaX = e.clientX - this.startX;
        const newWidth = Math.max(300, this.startWidth + deltaX); // Minimum width enforcement (300px)

        // Broadcast the new width to Vue
        this.player_.trigger('resizeDragging', { width: newWidth });
    }

    onMouseUp() {
        if (!this.isDragging) return;
        this.isDragging = false;

        window.removeEventListener('mousemove', this.boundOnMouseMove);
        window.removeEventListener('mouseup', this.boundOnMouseUp);

        this.player_.trigger('resizeEnd');
    }

    onKeyDown(e) {
        const resizeKeys = ['ArrowRight', 'ArrowLeft', '+', '-', '=', '_'];

        if (resizeKeys.includes(e.key)) {
            e.preventDefault();

            // Trigger start event on initial press down if not already active
            if (!this.isKeyResizing) {
                this.isKeyResizing = true;
                this.player_.trigger('resizeStart');
            }

            const step = e.shiftKey ? 50 : 20;
            let currentWidth = this.player_.el().parentElement.offsetWidth;

            if (e.key === 'ArrowRight' || e.key === '+' || e.key === '=') {
                const newWidth = Math.min(1920, currentWidth + step);
                this.player_.trigger('resizeDragging', { width: newWidth });
            } else if (e.key === 'ArrowLeft' || e.key === '-' || e.key === '_') {
                const newWidth = Math.max(300, currentWidth - step);
                this.player_.trigger('resizeDragging', { width: newWidth });
            }
        }
    }

    onKeyUp(e) {
        const resizeKeys = ['ArrowRight', 'ArrowLeft', '+', '-', '=', '_'];

        if (resizeKeys.includes(e.key)) {
            e.preventDefault();

            // Trigger end event when the sizing key is released
            if (this.isKeyResizing) {
                this.isKeyResizing = false;
                this.player_.trigger('resizeEnd');
            }
        }
    }
}

videojs.registerComponent('ResizeButton', ResizeButton);

// Register the plugin wrapper
function registerResizePlugin() {
    videojs.registerPlugin('resizeButton', function (options = {}) {
        this.ready(() => {
            // Add the ResizeButton to the controlBar if it isn't already there
            if (!this.controlBar.childNameIndex_['ResizeButton']) {
                this.controlBar.addChild('ResizeButton', options);
            }
        });
    });
}

export default registerResizePlugin;