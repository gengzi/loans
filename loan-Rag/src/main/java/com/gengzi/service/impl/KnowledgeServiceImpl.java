package com.gengzi.service.impl;


import com.gengzi.dao.Document;
import com.gengzi.dao.Knowledgebase;
import com.gengzi.dao.User;
import com.gengzi.dao.repository.DocumentRepository;
import com.gengzi.dao.repository.KnowledgebaseRepository;
import com.gengzi.dao.repository.UserRepository;
import com.gengzi.request.AddDocumentByS3;
import com.gengzi.request.KnowledgebaseCreateReq;
import com.gengzi.response.KnowledgebaseResponse;
import com.gengzi.s3.S3ClientUtils;
import com.gengzi.security.UserPrincipal;
import com.gengzi.service.KnowledgeService;
import com.gengzi.utils.IdUtils;
import com.gengzi.utils.UserDetails;
import jakarta.persistence.criteria.Predicate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.services.s3.model.S3Object;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class KnowledgeServiceImpl implements KnowledgeService {


    @Autowired
    private UserRepository userRepository;

    @Autowired
    private KnowledgebaseRepository knowledgebaseRepository;

    @Autowired
    private DocumentRepository documentRepository;

    @Autowired
    private S3ClientUtils s3ClientUtils;

    @Override
    public List<KnowledgebaseResponse> getKnowledgebase() {
        UserPrincipal user = UserDetails.getUser();
        Optional<User> userByDb = userRepository.findById(user.getId());
        if (userByDb.isPresent()) {
            String knowledgeIds = userByDb.get().getKnowledgeIds();
            if (knowledgeIds == null) {
                return List.of();
            }
            Set<String> knowledgeIdList = Arrays.stream(knowledgeIds.split(",")).collect(Collectors.toSet());
            List<Knowledgebase> allById = knowledgebaseRepository.findAllById(knowledgeIdList);
            return allById.stream().map(kb -> new KnowledgebaseResponse(kb.getId(), kb.getCreateTime(), kb.getCreateDate(), kb.getUpdateTime(),
                    kb.getUpdateDate(), kb.getAvatar(), kb.getName(), kb.getLanguage(), kb.getDescription(),
                    kb.getCreatedBy(), kb.getDocNum(), kb.getTokenNum(), kb.getChunkNum(), kb.getStatus())).collect(Collectors.toList());
        }
        return List.of();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createKnowledgebase(KnowledgebaseCreateReq knowledgebaseCreateReq) {
        Knowledgebase knowledgebase = new Knowledgebase();
        knowledgebase.setId(IdUtils.generateKnowledgeId());
        knowledgebase.setName(knowledgebaseCreateReq.getName());
        knowledgebase.setDescription(knowledgebaseCreateReq.getDescription());
        knowledgebase.setCreatedBy(UserDetails.getUser().getId());
        knowledgebase.setCreateTime(System.currentTimeMillis());
        knowledgebase.setCreateDate(Instant.now());
        knowledgebase.setUpdateTime(System.currentTimeMillis());
        knowledgebase.setUpdateDate(Instant.now());
        knowledgebase.setDocNum(0);
        knowledgebase.setTokenNum(0);
        knowledgebase.setChunkNum(0);
        knowledgebaseRepository.save(knowledgebase);
    }

    /**
     * @param kbId
     * @param pageable
     * @return
     */
    @Override
    public Page<?> documents(String kbId, Pageable pageable) {
        // 构建动态查询条件
        Specification<Document> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 条件1：名称模糊匹配（如果name不为空）
            if (kbId != null && !kbId.isEmpty()) {
                predicates.add(cb.equal(root.get("kbId"), kbId));
            }
            // 添加排序：按 updateTime 倒序（如果需要按 createTime 排序，替换字段名即可）
            query.orderBy(cb.desc(root.get("createDate")));
            // 组合所有条件
            return cb.and(predicates.toArray(new Predicate[0]));
        };
        return documentRepository.findAll(spec, pageable);

    }

    /**
     * @param addDocumentByS3
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void documentAdd(AddDocumentByS3 addDocumentByS3) {
        List<S3Object> s3Objects = s3ClientUtils.listFilesInDirectory(addDocumentByS3.getBucket(), addDocumentByS3.getKey());
        if (s3Objects != null && !s3Objects.isEmpty()){
            for (S3Object s3Object : s3Objects) {
                // 入库
                Document document = new Document();
                document.setId(IdUtils.generateDocumentId());
                document.setKbId(addDocumentByS3.getKbId());
                document.setName(s3Object.key().substring(s3Object.key().lastIndexOf("/") + 1));
                document.setLocation(s3Object.key());
                document.setSize(s3Object.size());
                document.setType(s3Object.storageClassAsString());
                document.setStatus("0");
                document.setRun("1");
                document.setCreateTime(System.currentTimeMillis());
                document.setCreateDate(Instant.now());
                document.setUpdateTime(System.currentTimeMillis());
                document.setUpdateDate(Instant.now());
                document.setSuffix(s3Object.key().substring(s3Object.key().lastIndexOf(".") + 1));
                document.setTokenNum(0);
                document.setChunkNum(0);
                document.setProgress(0.0f);
                document.setProgressMsg("");
                document.setProcessBeginAt(Instant.now());
                document.setProcessDuration(0.0f);
                document.setMetaFields("");
                document.setThumbnail("");
                document.setCreatedBy(UserDetails.getUser().getId());
                documentRepository.save(document);
                // 上传
                s3ClientUtils.copyObject( addDocumentByS3.getBucket(), s3Object.key(),
                        "rag", s3Object.key());
            }
        }


    }
}
