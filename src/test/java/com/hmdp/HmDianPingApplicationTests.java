package com.hmdp;

import cn.hutool.json.JSONUtil;
import com.hmdp.entity.Shop;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private StringRedisTemplate stringRedisTemplate;
    @Test
    public void testSaveShop(){
        shopService.saveShop2Redis(1L,5L);
    }
    @org.junit.jupiter.api.Test
    public void saveHotDataIn2Redis() {
        Shop shop = shopService.getById(1L);
        if(shop==null) return;
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(10L));
        stringRedisTemplate.opsForValue().set(RedisConstants.CACHE_SHOP_KEY+1,JSONUtil.toJsonStr(redisData));
    }


}
