package com.example.aikef.saas.aspect;

import com.example.aikef.saas.context.TenantContext;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.hibernate.Filter;
import org.hibernate.Session;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 50)
@Slf4j
public class TenantHibernateFilterAspect {

    @Value("${app.saas.enabled:false}")
    private boolean saasEnabled;

    @Autowired
    private EntityManager entityManager;

    @Before("execution(* com.example.aikef..repository..*Repository.*(..))")
    public void enableTenantFilter() {
        if (!saasEnabled) {
            return;
        }

        String tenantId = TenantContext.getTenantId();
        if (tenantId == null || tenantId.isBlank()) {
            return;
        }

        Session session = entityManager.unwrap(Session.class);
        Filter filter = session.getEnabledFilter("tenantFilter");
        if (filter == null) {
            filter = session.enableFilter("tenantFilter");
        }
        filter.setParameter("tenantId", tenantId);
    }
}

