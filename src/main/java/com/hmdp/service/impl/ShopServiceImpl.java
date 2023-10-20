package com.hmdp.service.impl;

import cn.hutool.core.util.BooleanUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.Shop;
import com.hmdp.mapper.ShopMapper;
import com.hmdp.service.IShopService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.RedisData;
import lombok.SneakyThrows;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.Exceptions;

import javax.annotation.Resource;

import java.sql.Time;
import java.time.LocalDateTime;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.hmdp.utils.RedisConstants.CACHE_SHOP_KEY;
import static com.hmdp.utils.RedisConstants.LOCK_SHOP_KEY;

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
//      缓存空值 缓存穿透  Shop shop = queryWithPassThrough(id);
      //  互斥锁解决缓存击穿
//        Shop shop = queryWithMuteThrough(id);
        //逻辑过期解决 缓存击穿
        Shop shop = queryWithLogicExpire(id);
        return Result.ok(shop);
    }
    @SneakyThrows
    private Shop queryWithMuteThrough(long id){
        String key = CACHE_SHOP_KEY+id;
        String key1 = LOCK_SHOP_KEY+id;
        while(true){
            String shopJson = stringRedisTemplate.opsForValue().get(key);
            if(StrUtil.isNotBlank(shopJson)){
                Shop shop = JSONUtil.toBean(shopJson, Shop.class);
                return shop;
            }
            if(tryLock(key1)){
                Shop shop = this.getById(id);
                if(shop==null){
                    stringRedisTemplate.opsForValue().set(key1,"",2,TimeUnit.MINUTES);
                    return null;
                }
                unlock(key1);
                return shop;
            }else{
                Thread.sleep(500);
            }
        }
    }
    private boolean tryLock(String key){
          Boolean flag = stringRedisTemplate.opsForValue().setIfAbsent(key,"1",10,TimeUnit.SECONDS);
          return BooleanUtil.isTrue(flag);
    }

    private void unlock(String key){
        stringRedisTemplate.delete(key);
    }
    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateByIdWithRedis(Shop shop) {
        String key = CACHE_SHOP_KEY + shop.getId();
        //1.更新数据库
        if(updateById(shop)){
            //2.删除缓存
            stringRedisTemplate.delete(key);
            return true;
        }else {
            return false;
        }
    }
//    @Transactional(rollbackFor = Exceptions.class)
    private static final ExecutorService CACHE_REBUILDER_EXECUTOR = Executors.newFixedThreadPool(10);
    private Shop queryWithLogicExpire(long id){
        String key = CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isBlank(shopJson)){
            //未命中说明不是热点信息 直接返回
            return null;
        }
        //命中则将json反序列化为对象
        RedisData redisData = JSONUtil.toBean(shopJson, RedisData.class);
        JSONObject data = (JSONObject)redisData.getData();
        Shop shop = JSONUtil.toBean(data, Shop.class);
        LocalDateTime expireTime = redisData.getExpireTime();
        //判断是否过期
        if(expireTime.isAfter(LocalDateTime.now())){
            //未过期则直接返回店铺信息
            return shop;
        }
        //过期则缓存重建 1.获取互斥锁
        String lockKey = LOCK_SHOP_KEY+id;
        boolean isLock = tryLock(lockKey);
        //2.判断是否获取成功
        if(isLock){
            //3,成功，开启线程实现缓存重建
            CACHE_REBUILDER_EXECUTOR.submit(()->{
                this.saveShop2Redis(id,20L);
                unlock(lockKey);
            });
        }
        //4.失败直接返回过期商铺信息
        return shop;
    }


    private Shop queryWithPassThrough(long id){
        String key = CACHE_SHOP_KEY+id;
        String shopJson = stringRedisTemplate.opsForValue().get(key);
        if(StrUtil.isNotBlank(shopJson)){
            Shop shop = JSONUtil.toBean(shopJson, Shop.class);
            return shop;
        }
        if(shopJson!=null){
            return null;
        }
        Shop shop = this.getById(id);
        if(shop==null){
            stringRedisTemplate.opsForValue().set(key,"",2,TimeUnit.MINUTES);
            return null;
        }
        stringRedisTemplate.opsForValue().set(key,JSONUtil.toJsonStr(shop),30L,TimeUnit.MINUTES);
        return null;
    }

    public void saveShop2Redis(Long id,Long expireTime){
        Shop shop = getById(id);
        RedisData redisData = new RedisData();
        redisData.setData(shop);
        redisData.setExpireTime(LocalDateTime.now().plusSeconds(expireTime));
        stringRedisTemplate.opsForValue().set(CACHE_SHOP_KEY+id,JSONUtil.toJsonStr(redisData));
    }
}
