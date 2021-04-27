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
import java.time.temporal.TemporalUnit
import java.util.concurrent.TimeUnit

@Configuration
class CachingConfiguration implements WebMvcConfigurer{



    public static final Duration MAX_AGE = Duration.of(14, ChronoUnit.DAYS)
    public static final Duration LONG_MAX_AGE = Duration.of(90, ChronoUnit.DAYS)

    @Override
    void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/static/*.ico")
                .addResourceLocations("classpath:/public/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/js/**")
                .addResourceLocations("classpath:/public/static/js/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/css/**")
                .addResourceLocations("classpath:/public/static/css/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/img/**")
                .addResourceLocations("classpath:/public/static/img/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/fonts/**")
                .addResourceLocations("classpath:/public/static/fonts/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/clientPortal/css/**")
                .addResourceLocations("classpath:/public/static/clientPortal/css/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/clientPortal/js/**")
                .addResourceLocations("classpath:/public/static/clientPortal/js/")
                .setCacheControl(CacheControl.maxAge(LONG_MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/clientPortal/img/**")
                .addResourceLocations("classpath:/public/static/clientPortal/img/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/clientPortal/fonts/**")
                .addResourceLocations("classpath:/public/static/clientPortal/fonts/")
                .setCacheControl(CacheControl.maxAge(MAX_AGE).cachePrivate().mustRevalidate())
        registry.addResourceHandler("/static/**/*.html")
                .addResourceLocations("classpath:/public/static/clientPortal/", "classpath:/public/static/")
                .setCacheControl(CacheControl.noStore())

    }
}
