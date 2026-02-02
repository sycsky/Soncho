package com.example.aikef.saas.config;

import com.example.aikef.saas.interceptor.TenantInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.core.Ordered;
import com.example.aikef.saas.repository.TenantAwareSimpleJpaRepository;

@Configuration
@EnableTransactionManagement(order = Ordered.HIGHEST_PRECEDENCE)
@EnableJpaRepositories(
        basePackages = "com.example.aikef",
        repositoryBaseClass = TenantAwareSimpleJpaRepository.class
)
public class SaasConfig implements WebMvcConfigurer {

    @Autowired
    private TenantInterceptor tenantInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(tenantInterceptor)
                .addPathPatterns("/api/**");
    }
}
