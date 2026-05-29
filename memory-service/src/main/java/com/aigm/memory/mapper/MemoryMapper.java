package com.aigm.memory.mapper;

import com.aigm.memory.entity.Memory;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.List;

/**
 * t_memory 数据访问（降级表）：embedding 以 REAL[]（float4[]）存取，
 * 余弦相似度在应用层（service）计算，不依赖 pgvector。
 */
@Repository
@RequiredArgsConstructor
public class MemoryMapper {

    private final JdbcTemplate jdbcTemplate;

    /** 插入一条记忆，embedding 经 createArrayOf("float4", ...) 作为 REAL[] 写入。 */
    public void insert(Long sessionId, String content, float[] embedding, String memType, Integer importance) {
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(
                    "INSERT INTO t_memory (session_id, content, embedding, mem_type, importance) " +
                            "VALUES (?, ?, ?, ?, ?)");
            ps.setLong(1, sessionId);
            ps.setString(2, content);
            ps.setArray(3, toFloatArray(con, embedding));
            ps.setString(4, memType);
            ps.setInt(5, importance);
            return ps;
        });
    }

    /** 取某 session 的全部记忆（含 embedding），供应用层暴力余弦召回。 */
    public List<Memory> findBySession(Long sessionId) {
        return jdbcTemplate.query(
                "SELECT id, session_id, content, embedding, mem_type, importance, created_at " +
                        "FROM t_memory WHERE session_id = ?",
                (rs, rowNum) -> {
                    Memory m = new Memory();
                    m.setId(rs.getLong("id"));
                    m.setSessionId(rs.getLong("session_id"));
                    m.setContent(rs.getString("content"));
                    m.setEmbedding(readFloatArray(rs.getArray("embedding")));
                    m.setMemType(rs.getString("mem_type"));
                    m.setImportance(rs.getInt("importance"));
                    java.sql.Timestamp ts = rs.getTimestamp("created_at");
                    m.setCreatedAt(ts == null ? null : ts.toLocalDateTime());
                    return m;
                },
                sessionId);
    }

    private Array toFloatArray(Connection con, float[] embedding) throws java.sql.SQLException {
        Float[] boxed = new Float[embedding.length];
        for (int i = 0; i < embedding.length; i++) {
            boxed[i] = embedding[i];
        }
        return con.createArrayOf("float4", boxed);
    }

    private float[] readFloatArray(Array array) throws java.sql.SQLException {
        if (array == null) {
            return new float[0];
        }
        Object raw = array.getArray();
        if (raw instanceof Float[] boxed) {
            float[] out = new float[boxed.length];
            for (int i = 0; i < boxed.length; i++) {
                out[i] = boxed[i] == null ? 0f : boxed[i];
            }
            return out;
        }
        if (raw instanceof Number[] nums) {
            float[] out = new float[nums.length];
            for (int i = 0; i < nums.length; i++) {
                out[i] = nums[i] == null ? 0f : nums[i].floatValue();
            }
            return out;
        }
        return new float[0];
    }
}
