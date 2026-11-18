package com.bnaqica.crmdemocoreservice.config;

import com.bnaqica.crmdemocoreservice.processors.ActiveUserInterceptor;
import com.bnaqica.crmdemocoreservice.processors.PermissionInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final PermissionInterceptor permissionInterceptor;
    private final ActiveUserInterceptor activeUserInterceptor;

    /** The permission first (local and cheap), then the UMS status. */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(permissionInterceptor);
        registry.addInterceptor(activeUserInterceptor);
    }
}
