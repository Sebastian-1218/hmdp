package com.hmdp;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.Shop;
import com.hmdp.entity.User;
import com.hmdp.service.impl.BlogServiceImpl;
import com.hmdp.service.impl.ShopServiceImpl;
import com.hmdp.service.impl.UserServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import org.junit.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;
import static jdk.nashorn.internal.runtime.regexp.joni.Config.log;

@SpringBootTest
class HmDianPingApplicationTests {
    @Resource
    private ShopServiceImpl shopService;
    @Resource
    private UserServiceImpl userService;
    @Resource
    private BlogServiceImpl blogService;

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

    @org.junit.jupiter.api.Test
    public void saveHotDataIn2Redis1() {
        Blog blog = blogService.getById(4);
        User user = userService.getById(4);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
        log.print(blog);
    }
    @org.junit.jupiter.api.Test
    public void saveHotDataIn2Redis2() {
        String key = BLOG_LIKED_KEY + 4;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key,0,4);
        if(top5==null){
            return;
        }
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String join = StrUtil.join(",",ids);
        System.out.println(join);
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("order by field(id," + join + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
    }

}
