package com.aigm.scenario.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 剧本旗标定义 t_flag_def：key + 含义 + 默认值，供运行时「调查手记」按 key 展示可读名。 */
@Data
@TableName("t_flag_def")
public class FlagDef {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long scenarioId;
    private String flagKey;
    private String flagName;
    private String defaultValue;
}
