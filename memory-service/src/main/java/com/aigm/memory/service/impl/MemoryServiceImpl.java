package com.aigm.memory.service.impl;

import com.aigm.common.exception.BizException;
import com.aigm.common.feign.dto.MemoryItem;
import com.aigm.common.feign.dto.RecallRequest;
import com.aigm.common.feign.dto.RecallResult;
import com.aigm.common.feign.dto.RecalledMemory;
import com.aigm.common.feign.dto.StoreRequest;
import com.aigm.common.feign.dto.StoreResult;
import com.aigm.common.result.ResultCode;
import com.aigm.memory.config.MemoryProps;
import com.aigm.memory.entity.Memory;
import com.aigm.memory.mapper.MemoryMapper;
import com.aigm.memory.service.EmbeddingClient;
import com.aigm.memory.service.MemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** 存/召回编排：embedding + REAL[] 落库 + 应用层余弦召回（importance 加权）。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MemoryServiceImpl implements MemoryService {

    private static final String DEFAULT_MEM_TYPE = "EVENT";
    private static final int DEFAULT_IMPORTANCE = 3;

    private final EmbeddingClient embeddingClient;
    private final MemoryMapper memoryMapper;
    private final MemoryProps memoryProps;

    @Override
    public StoreResult store(StoreRequest request) {
        if (request == null || request.getSessionId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "sessionId 不能为空");
        }
        StoreResult result = new StoreResult();
        if (request.getItems() == null || request.getItems().isEmpty()) {
            result.setStoredCount(0);
            return result;
        }
        int stored = 0;
        for (MemoryItem item : request.getItems()) {
            if (item == null || !StringUtils.hasText(item.getContent())) {
                continue;
            }
            float[] embedding = embeddingClient.embed(item.getContent());
            String memType = StringUtils.hasText(item.getMemType()) ? item.getMemType() : DEFAULT_MEM_TYPE;
            int importance = item.getImportance() == null ? DEFAULT_IMPORTANCE : item.getImportance();
            memoryMapper.insert(request.getSessionId(), item.getContent(), embedding, memType, importance);
            stored++;
        }
        result.setStoredCount(stored);
        return result;
    }

    @Override
    public RecallResult recall(RecallRequest request) {
        if (request == null || request.getSessionId() == null) {
            throw new BizException(ResultCode.PARAM_MISSING, "sessionId 不能为空");
        }
        int topK = (request.getTopK() == null || request.getTopK() <= 0)
                ? memoryProps.getRecallDefaultTopK()
                : request.getTopK();

        RecallResult result = new RecallResult();
        if (!StringUtils.hasText(request.getQuery())) {
            result.setMemories(new ArrayList<>());
            return result;
        }

        List<Memory> candidates = memoryMapper.findBySession(request.getSessionId());
        if (candidates.isEmpty()) {
            result.setMemories(new ArrayList<>());
            return result;
        }

        float[] queryVec = embeddingClient.embed(request.getQuery());

        List<RecalledMemory> scored = new ArrayList<>(candidates.size());
        for (Memory m : candidates) {
            double cosine = cosine(queryVec, m.getEmbedding());
            int importance = m.getImportance() == null ? DEFAULT_IMPORTANCE : m.getImportance();
            double score = cosine * (1 + importance / 10.0);
            RecalledMemory rm = new RecalledMemory();
            rm.setContent(m.getContent());
            rm.setMemType(m.getMemType());
            rm.setImportance(importance);
            rm.setScore(score);
            scored.add(rm);
        }

        scored.sort(Comparator.comparingDouble(RecalledMemory::getScore).reversed());
        List<RecalledMemory> top = scored.size() > topK ? scored.subList(0, topK) : scored;
        result.setMemories(new ArrayList<>(top));
        return result;
    }

    /** 余弦相似度；维度不一致或零向量时返回 0。 */
    private double cosine(float[] a, float[] b) {
        if (a == null || b == null || a.length == 0 || b.length == 0 || a.length != b.length) {
            return 0.0;
        }
        double dot = 0.0;
        double na = 0.0;
        double nb = 0.0;
        for (int i = 0; i < a.length; i++) {
            dot += (double) a[i] * b[i];
            na += (double) a[i] * a[i];
            nb += (double) b[i] * b[i];
        }
        if (na == 0.0 || nb == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(na) * Math.sqrt(nb));
    }
}
