package com.aigm.ai.llm;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** OpenAI 兼容 message（基线 §5.3.3）。role: system|user|assistant。 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatMessage {
    private String role;
    private String content;
}
