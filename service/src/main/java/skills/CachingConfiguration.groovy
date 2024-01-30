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
import org.springframework.http.CacheControl
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer

import java.time.Duration
import java.time.temporal.ChronoUnit

@Configuration
class CachingConfiguration implements WebMvcConfigurer {

    public static final Duration MAX_AGE = Duration.of(14, ChronoUnit.DAYS)
    public static final Duration LONG_MAX_AGE = Duration.of(90, ChronoUnit.DAYS)

    @Override
    void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/*.ico")
                .addResourceLocations("classpath:/public/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/js/**")
                .addResourceLocations("classpath:/public/assets/js/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/css/**")
                .addResourceLocations("classpath:/public/assets/css/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/img/**")
                .addResourceLocations("classpath:/public/assets/img/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/fonts/**")
                .addResourceLocations("classpath:/public/assets/fonts/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/clientPortal/css/**")
                .addResourceLocations("classpath:/public/assets/clientPortal/css/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/clientPortal/js/**")
                .addResourceLocations("classpath:/public/assets/clientPortal/js/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/clientPortal/img/**")
                .addResourceLocations("classpath:/public/assets/clientPortal/img/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/clientPortal/fonts/**")
                .addResourceLocations("classpath:/public/assets/clientPortal/fonts/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/assets/clientPortal/*.html")
                .addResourceLocations("classpath:/public/assets/clientPortal/")
                .setCacheControl(CacheControl.noStore())
        registry.addResourceHandler("/*.html")
                .addResourceLocations("classpath:/public/")
                .setCacheControl(CacheControl.noStore())

    }
}
