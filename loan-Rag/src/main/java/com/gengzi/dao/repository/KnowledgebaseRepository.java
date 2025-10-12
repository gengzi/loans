package com.gengzi.dao.repository;

import com.gengzi.dao.Knowledgebase;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface KnowledgebaseRepository extends JpaRepository<Knowledgebase, String>, JpaSpecificationExecutor<Knowledgebase> {
}