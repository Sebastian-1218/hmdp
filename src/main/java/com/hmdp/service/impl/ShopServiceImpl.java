package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopServiceImpl extends ServiceImpl<ShopMapper, Shop> implements IShopService {
    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryById(Long id) {
        String key = CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return Result.ok(shop);
        }
        Shop shop = this.getById(id);
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));
        return Result.ok(shop);
    }

    private boolean trylock(String key){
        Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1", 10, TimeUnit.SECONDS);
        return BooleanUtil.isTrue(flag);
    }
    private void free(String key){
        stringRedisTemplate.delete(key);
    }
//    @Override
//    public Result queryById(Long id) {
//        String key = CACHE_SHOP_KEY+id;
//        //1.从redis查询商铺缓存
//        String shopJson = stringRedisTemplate.opsForValue().get(key);
//        //2.判断是否存在
//        if (StrUtil.isNotBlank(shopJson)) {
//            //3.存在 直接返回
//            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
//            return Result.ok(shop);
//        }
//        //4.查询数据库 不存在返回错误
//        Shop shop = getById(id);
//        if(shop == null){
//            return Result.fail("不存在商铺");
//        }
//        //5.存在写入redis返回
//        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop));
//        return Result.ok(shop);
//    }
}
