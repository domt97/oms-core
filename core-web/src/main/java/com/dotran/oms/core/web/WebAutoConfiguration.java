package com.dotran.oms.core.web;

import com.dotran.oms.core.web.advice.GlobalExceptionHandler;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.context.annotation.Import;

@AutoConfiguration
@Import(GlobalExceptionHandler.class)
public class WebAutoConfiguration {
}
