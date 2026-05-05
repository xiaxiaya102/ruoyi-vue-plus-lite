package org.dromara.system.controller.monitor;

import cn.dev33.satoken.annotation.SaCheckPermission;
import lombok.RequiredArgsConstructor;
import org.dromara.common.core.domain.R;
import org.dromara.common.redis.utils.RedisUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * 缓存监控
 *
 * @author Lion Li
 */
@RequiredArgsConstructor
@RestController
@RequestMapping("/monitor/cache")
public class CacheController {

    /**
     * 获取缓存监控列表
     */
    @SaCheckPermission("monitor:cache:list")
    @GetMapping()
    public R<CacheListInfoVo> getInfo() {
        return R.ok(new CacheListInfoVo(RedisUtils.localInfo(), RedisUtils.dbSize(), new ArrayList<>()));
    }

    /**
     * 缓存监控列表信息
     *
     * @param info         信息
     * @param dbSize       数据库
     * @param commandStats 命令统计
     */
    public record CacheListInfoVo(Properties info, Long dbSize, List<Map<String, String>> commandStats) {}

}
