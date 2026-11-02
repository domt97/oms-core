package com.dotran.oms.core.rest.logging;

import com.dotran.oms.core.logging.MdcProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link MdcRequestFieldsInterceptor} for every handler (found by the services' component scan). */
@Configuration
@RequiredArgsConstructor
public class MdcWebMvcConfig implements WebMvcConfigurer {

    private final MdcProperties mdcProperties;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MdcRequestFieldsInterceptor(mdcProperties));
    }
}
