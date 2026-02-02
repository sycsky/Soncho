package com.example.aikef.saas.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.Assert;

import java.io.Serializable;
import java.util.List;
import java.util.Optional;

public class TenantAwareSimpleJpaRepository<T, ID extends Serializable> extends SimpleJpaRepository<T, ID> {

    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;

    public TenantAwareSimpleJpaRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<T> findById(ID id) {
        Assert.notNull(id, "The given id must not be null");

        String entityName = entityInformation.getEntityName();
        String idAttribute = entityInformation.getIdAttribute().getName();
        String jpql = "select e from " + entityName + " e where e." + idAttribute + " = :id";
        TypedQuery<T> query = entityManager.createQuery(jpql, getDomainClass());
        query.setParameter("id", id);
        query.setMaxResults(1);
        List<T> results = query.getResultList();
        return results.stream().findFirst();
    }

    @Override
    public boolean existsById(ID id) {
        Assert.notNull(id, "The given id must not be null");

        String entityName = entityInformation.getEntityName();
        String idAttribute = entityInformation.getIdAttribute().getName();
        String jpql = "select count(e) from " + entityName + " e where e." + idAttribute + " = :id";
        TypedQuery<Long> query = entityManager.createQuery(jpql, Long.class);
        query.setParameter("id", id);
        return query.getSingleResult() > 0;
    }
}

