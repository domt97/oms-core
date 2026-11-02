package com.dotran.oms.core.rest.logging;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/** Registers {@link MdcPathVariableInterceptor} for every handler (found by the services' component scan). */
@Configuration
public class MdcWebMvcConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new MdcPathVariableInterceptor());
    }
}
