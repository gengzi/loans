package com.gengzi.dao.repository;

import com.gengzi.dao.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentRepository extends JpaRepository<Document, String>, JpaSpecificationExecutor<Document> {
}