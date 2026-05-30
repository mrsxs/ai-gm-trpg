package com.aigm.memory.service;

import com.aigm.common.exception.BizException;
import com.aigm.common.result.ResultCode;
import com.aigm.memory.config.EmbeddingProps;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;

/**
 * 嵌入向量生成：api-key 非空时调 OpenAI 兼容 /embeddings；为空时走确定性伪向量兜底。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmbeddingClient {

    private final EmbeddingProps props;
    private final RestClient embeddingRestClient;

    /** 生成 content 的 embedding，长度 = props.dimension(1024)。 */
    public float[] embed(String content) {
        String text = content == null ? "" : content;
        if (StringUtils.hasText(props.getApiKey())) {
            return embedViaApi(text);
        }
        return pseudoEmbed(text);
    }

    @SuppressWarnings("unchecked")
    private float[] embedViaApi(String text) {
        String url = trimTrailingSlash(props.getBaseUrl()) + "/embeddings";
        try {
            // dimensions 锁死 1024：DashScope text-embedding-v3/v4 与 OpenAI v3 均支持，
            // 保证返回维度与 pgvector 列定义严格一致（基线：向量维度严格 1024）。
            Map<String, Object> body = Map.of(
                    "model", props.getModel(),
                    "input", text,
                    "dimensions", props.getDimension(),
                    "encoding_format", "float"
            );
            Map<String, Object> resp = embeddingRestClient.post()
                    .uri(url)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + props.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);
            if (resp == null) {
                throw new BizException(ResultCode.AI_EMBEDDING_FAILED, "嵌入响应为空");
            }
            List<Map<String, Object>> data = (List<Map<String, Object>>) resp.get("data");
            if (data == null || data.isEmpty()) {
                throw new BizException(ResultCode.AI_EMBEDDING_FAILED, "嵌入响应缺少 data");
            }
            List<Number> arr = (List<Number>) data.get(0).get("embedding");
            if (arr == null || arr.isEmpty()) {
                throw new BizException(ResultCode.AI_EMBEDDING_FAILED, "嵌入响应缺少 embedding");
            }
            if (arr.size() != props.getDimension()) {
                throw new BizException(ResultCode.AI_EMBEDDING_FAILED,
                        "嵌入维度不符: expected " + props.getDimension() + ", got " + arr.size());
            }
            float[] vec = new float[arr.size()];
            for (int i = 0; i < arr.size(); i++) {
                vec[i] = arr.get(i).floatValue();
            }
            return vec;
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            log.warn("调用 embedding API 失败: {}", e.getMessage());
            throw new BizException(ResultCode.AI_EMBEDDING_FAILED, "调用嵌入服务失败");
        }
    }

    /**
     * 确定性伪向量兜底：相同文本得相同向量、不同文本不同向量；L2 归一化。
     * 每个维度由 sha256(content + ":" + i) 映射到 [-1,1]。可用于演示语义召回的自相似 > 0。
     */
    private float[] pseudoEmbed(String text) {
        int dim = props.getDimension();
        float[] vec = new float[dim];
        double norm = 0.0;
        for (int i = 0; i < dim; i++) {
            long h = hash(text + ":" + i);
            // 映射到 [-1,1]
            double v = (h & 0xFFFFFFFFL) / (double) 0xFFFFFFFFL * 2.0 - 1.0;
            vec[i] = (float) v;
            norm += v * v;
        }
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                vec[i] = (float) (vec[i] / norm);
            }
        }
        return vec;
    }

    private long hash(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            long h = 0L;
            for (int i = 0; i < 8; i++) {
                h = (h << 8) | (d[i] & 0xFFL);
            }
            return h;
        } catch (NoSuchAlgorithmException e) {
            return s.hashCode();
        }
    }

    private String trimTrailingSlash(String s) {
        if (s == null) {
            return "";
        }
        return s.endsWith("/") ? s.substring(0, s.length() - 1) : s;
    }
}
