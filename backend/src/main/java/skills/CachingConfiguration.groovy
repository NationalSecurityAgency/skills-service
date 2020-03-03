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
