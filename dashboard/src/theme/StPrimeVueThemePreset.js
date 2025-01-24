/*
 * Copyright 2024 SkillTree
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
import {definePreset} from "@primevue/themes";
import Aura from "@primevue/themes/aura";

const colorScheme = {
    light: {
        primary: {
            color: '{green.800}',
                inverseColor: '#ffffff',
                hoverColor: '{green.600}',
                activeColor: '{green.700}'
        },
        highlight: {
            background: '{green.800}',
                focusBackground: '{green.800}',
                color: '#ffffff',
                focusColor: '#ffffff'
        }
    },
}

const button = {
    colorScheme: {
        light: {
            outlined: {
                info: {
                    borderColor: '{sky.300}',
                        color: '{sky.800}'
                },
                primary: {
                    borderColor: '{green.600}',
                }
            },
        },
    }
}

const badge = {
    colorScheme: {
        light: {
            info: {
                background: '{sky.700}',
                color: '{surface.0}'
            },
        },
    }
}


const defineSkillTreePreset = () => {
    return definePreset(Aura, {
        semantic: { colorScheme },
        components: { button, badge }
    });
}

export default defineSkillTreePreset