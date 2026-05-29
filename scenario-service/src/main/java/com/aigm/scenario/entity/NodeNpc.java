package com.aigm.scenario.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

@Data
@TableName("t_node_npc")
public class NodeNpc {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long nodeId;
    private Long npcId;
}
