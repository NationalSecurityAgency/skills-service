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
    dark: {
        primary: {
        },
        highlight: {
        },
        surface: {
            0: '#ffffff',
            50: '{slate.50}',
            100: '{slate.100}',
            200: '{slate.200}',
            300: '{slate.300}',
            400: '{slate.400}',
            500: '{slate.500}',
            600: '{slate.600}',
            700: '{slate.700}',
            800: '{slate.800}',
            900: '{slate.900}',
            950: '{slate.950}'
        }
    }
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
                },
                success: {
                    borderColor: '{green.300}',
                    color: '{green.800}'
                },
                danger: {
                    borderColor: '{red.300}',
                    color: '{red.800}'
                },
                warn: {
                    borderColor: '{orange.300}',
                    color: '{orange.800}'
                },
                help: {
                    borderColor: '{purple.300}',
                    color: '{purple.800}'
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

const message = {
    colorScheme: {
        light: {
            success: {
                color: '{green.700}',
            },
            info: {
                color: '{blue.700}',
            },
            warn: {
                color: '{yellow.700}',
            }
        },
        dark: {
            info: {
                background: 'color-mix(in srgb, {blue.500}, transparent 84%)',
                color: '{blue.400}',
            },
        }
    }
}

const card = {
    root: {
        borderRadius: '{border.radius.md}',
    },
}

const defineSkillTreePreset = () => {
    return definePreset(Aura, {
        semantic: { colorScheme },
        components: { button, badge, message, card }
    });
}

export default defineSkillTreePreset