package com.aigm.ai.controller;

import com.aigm.ai.config.LlmProperties;
import com.aigm.ai.llm.ChatMessage;
import com.aigm.ai.service.LlmClient;
import com.aigm.ai.service.OutputValidator;
import com.aigm.ai.service.PromptBuilder;
import com.aigm.ai.service.StubGenerator;
import com.aigm.common.exception.BizException;
import com.aigm.common.feign.dto.GenerateRequest;
import com.aigm.common.feign.dto.GenerateResponse;
import com.aigm.common.result.R;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * AI 引擎入口（基线 §5.9）：POST /api/ai/generate（INTERNAL，无需鉴权）。
 * api-key 为空走确定性 STUB；否则走真实 LLM。两条路径都过 OutputValidator。
 * 失败也返回兜底 GenerateResponse（code=0），不向上游抛 500。
 */
@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiGenerateController {

    private final PromptBuilder promptBuilder;
    private final LlmClient llmClient;
    private final OutputValidator outputValidator;
    private final StubGenerator stubGenerator;
    private final LlmProperties llmProperties;

    @PostMapping("/generate")
    public R<GenerateResponse> generate(@RequestBody GenerateRequest req) {
        long start = System.currentTimeMillis();
        try {
            GenerateResponse resp;
            if (llmProperties.hasApiKey()) {
                List<ChatMessage> messages = promptBuilder.build(req);
                LlmClient.Result llmResult = llmClient.chatForJson(messages);
                resp = outputValidator.validate(llmResult, req);
            } else {
                // 无 key 兜底：确定性 STUB，不调用网络
                GenerateResponse stub = stubGenerator.generate(req);
                resp = outputValidator.validateStructured(stub, req);
            }
            log.info("[AI] session={} done in {}ms stub={} fallback={}",
                    req.getSessionId(), System.currentTimeMillis() - start,
                    !llmProperties.hasApiKey(),
                    resp.getValidation() == null ? null : resp.getValidation().get("fallback"));
            return R.ok(resp);
        } catch (BizException e) {
            // LLM 调用彻底失败(1500)：返回兜底，保证上游可继续
            log.error("[AI] session={} biz error code={} msg={}",
                    req.getSessionId(), e.getCode(), e.getMessage());
            GenerateResponse fb = outputValidator.validate(new LlmClient.Result("", false, 0), req);
            if (fb.getValidation() != null) fb.getValidation().put("rejectCode", e.getCode());
            return R.ok(fb);
        }
    }
}
