package com.iceeboot.framework.service.system.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.iceeboot.common.constant.SystemConstants;
import com.iceeboot.framework.entity.system.SocialDO;
import com.iceeboot.framework.mapper.system.SocialMapper;
import com.iceeboot.framework.service.system.ISocialService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 第三方登录接口实现
 *
 * @author CodeIcee
 */
@Service
@RequiredArgsConstructor
public class SocialServiceImpl extends ServiceImpl<SocialMapper, SocialDO> implements ISocialService {

    private final SocialMapper socialMapper;


    @Override
    public SocialDO findByOpenIdAndPlatform(String openId, Integer platform) {
        LambdaQueryWrapper<SocialDO> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SocialDO::getOpenId,openId);
        wrapper.eq(SocialDO::getPlatform,platform);
        wrapper.eq(SocialDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED);

        return socialMapper.selectOne(wrapper);
    }

    @Override
    public SocialDO findByRelateUsernameAndPlatform(String relateUsername, Integer platform) {
        LambdaQueryWrapper<SocialDO> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SocialDO::getUsername,relateUsername);
        wrapper.eq(SocialDO::getPlatform,platform);
        wrapper.eq(SocialDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED);
        return socialMapper.selectOne(wrapper);
    }

    @Override
    public List<SocialDO> findByRelateUsername(String relateUsername) {
        LambdaQueryWrapper<SocialDO> wrapper=new LambdaQueryWrapper<>();
        wrapper.eq(SocialDO::getUsername,relateUsername);
        wrapper.eq(SocialDO::getDelFlag, SystemConstants.DeleteFlag.NOT_DELETED);
        return socialMapper.selectList(wrapper);
    }


}