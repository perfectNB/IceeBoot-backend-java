package com.iceeboot.framework.mapper.system;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.iceeboot.framework.entity.system.SocialDO;
import org.apache.ibatis.annotations.Mapper;

/**
 * 第三方登录登录数据处理层
 * @author CodeIcee
 */
@Mapper
public interface SocialMapper extends BaseMapper<SocialDO> {


}