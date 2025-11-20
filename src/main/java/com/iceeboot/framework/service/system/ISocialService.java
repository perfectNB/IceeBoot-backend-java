package com.iceeboot.framework.service.system;

import com.baomidou.mybatisplus.extension.service.IService;
import com.iceeboot.framework.entity.system.SocialDO;

import java.util.List;

/**
 * 第三方登录接口实现
 * @author CodeIcee
 */
public interface ISocialService extends IService<SocialDO> {

    /**
     * 通过openId和平台获取
     * @param openId
     * @param platform
     * @return
     */
    SocialDO findByOpenIdAndPlatform(String openId, Integer platform);

    /**
     * 通过userId和平台获取
     * @param relateUsername
     * @param platform
     * @return
     */
    SocialDO findByRelateUsernameAndPlatform(String relateUsername, Integer platform);

    /**
     * 通过relateUsername获取
     * @param relateUsername
     * @return
     */
    List<SocialDO> findByRelateUsername(String relateUsername);

//    /**
//     * 分页多条件获取
//     * @param social
//     * @param searchVo
//     * @param pageable
//     * @return
//     */
//    Page<SocialDO> findByCondition(SocialDO social, SearchVo searchVo, Pageable pageable);
}