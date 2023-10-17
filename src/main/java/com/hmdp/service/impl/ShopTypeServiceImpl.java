package com.hmdp.service.impl;

import cn.hutool.json.JSONUtil;
import com.hmdp.dto.Result;
import com.hmdp.entity.ShopType;
import com.hmdp.mapper.ShopTypeMapper;
import com.hmdp.service.IShopTypeService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;


import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_LIST_KEY;
import static com.hmdp.utils.RedisConstants.CACHE_SHOP_TYPE_LIST_KEY1;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class ShopTypeServiceImpl extends ServiceImpl<ShopTypeMapper, ShopType> implements IShopTypeService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;
//    @Override
//    public Result queryList() {
//        String key = CACHE_SHOP_TYPE_LIST_KEY1;
//        //1.从redis中查询缓存，没命中就查询数据库，并写入redis，命中则直接返回
//        List<String> category = stringRedisTemplate.opsForList().range(key, 0, -1);
//        if(category!=null && !category.isEmpty() ){
//            ArrayList<ShopType> typeList = new ArrayList<>();
//            for (String str:category){
//                typeList.add(JSONUtil.toBean(str"","ShopType.class"));
//            }
//            return Result.ok(typeList);
//        }
//        List<ShopType> typeList = this.query().orderByAsc("sort").list();
//        for (ShopType shopType: typeList) {
//            stringRedisTemplate.opsForList().leftPush(key,shopType.toString());
//        }
//
//        return Result.ok(typeList);
//    }
    @Override
    public Result queryList() {
        //1.查询redis缓存
        List<String> shopTypeListInRedis = stringRedisTemplate.opsForList().range(CACHE_SHOP_TYPE_LIST_KEY, 0, -1);

        //2.判断是否存在
        if(!shopTypeListInRedis.isEmpty()){
            //3.存在就返回
            List<ShopType> shopTypeList = shopTypeListInRedis.stream().map(item -> {
                return JSONUtil.toBean(item, ShopType.class);
            }).collect(Collectors.toList());

            return Result.ok(shopTypeList);
        }

        //4.不存在，查询数据库
        List<ShopType> shopTypeList = query().orderByAsc("sort").list();
        List<String> resList = shopTypeList.stream().map(item -> {
            return JSONUtil.toJsonStr(item);
        }).collect(Collectors.toList());

        //5.将查询出来写入redis
        stringRedisTemplate.opsForList().rightPushAll(CACHE_SHOP_TYPE_LIST_KEY, resList);

        //6.将查询出的结果返回
        return Result.ok(shopTypeList);
    }
}
