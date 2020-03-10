/**
 * Copyright 2020 SkillTree
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
package skills

import org.springframework.context.annotation.Configuration
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

import java.util.concurrent.TimeUnit

@Configuration
class CachingConfiguration implements WebMvcConfigurer{



    public static final int CACHE_TIME_IN_SEC = TimeUnit.DAYS.toSeconds(14)

    @Override
    void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/js/**")
                .addResourceLocations("classpath:/public/static/js/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
        registry.addResourceHandler("/static/css/**")
                .addResourceLocations("classpath:/public/static/css/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
        registry.addResourceHandler("/static/img/**")
                .addResourceLocations("classpath:/public/static/img/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
        registry.addResourceHandler("/static/fonts/**")
                .addResourceLocations("classpath:/public/static/fonts/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
        registry.addResourceHandler("/static/clientPortal/css/**")
                .addResourceLocations("classpath:/public/static/clientPortal/css/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
        registry.addResourceHandler("/static/clientPortal/js/**")
                .addResourceLocations("classpath:/public/static/clientPortal/js/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
        registry.addResourceHandler("/static/clientPortal/img/**")
                .addResourceLocations("classpath:/public/static/clientPortal/img/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
        registry.addResourceHandler("/static/clientPortal/fonts/**")
                .addResourceLocations("classpath:/public/static/clientPortal/fonts/")
                .setCachePeriod(CACHE_TIME_IN_SEC)
    }
}
