package com.hmdp.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.hmdp.dto.Result;
import com.hmdp.dto.UserDTO;
import com.hmdp.entity.Blog;
import com.hmdp.entity.User;
import com.hmdp.mapper.BlogMapper;
import com.hmdp.service.IBlogService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.service.IUserService;
import com.hmdp.utils.RedisConstants;
import com.hmdp.utils.SystemConstants;
import com.hmdp.utils.UserHolder;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.hmdp.utils.RedisConstants.BLOG_LIKED_KEY;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class BlogServiceImpl extends ServiceImpl<BlogMapper, Blog> implements IBlogService {

    @Resource
    private IUserService userService;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Override
    public Result queryBlogById(Long id) {
        Blog blog = this.getById(id);
        User user = userService.getById(id);
        blog.setIcon(user.getIcon());
        blog.setName(user.getNickName());
        return Result.ok(blog);
    }

    @Override
    public Result queryHotBlog(Integer current) {
        // 根据用户查询
        Page<Blog> page = this.query()
                .orderByDesc("liked")
                .page(new Page<>(current, SystemConstants.MAX_PAGE_SIZE));
        // 获取当前页数据
        List<Blog> records = page.getRecords();
        // 查询用户
        records.forEach(blog ->{
            Long userId = blog.getUserId();
            User user = userService.getById(userId);
            blog.setName(user.getNickName());
            blog.setIcon(user.getIcon());
            this.isBlogLinked(blog);
        });
        return Result.ok(records);
    }

    @Override
    public Result islikeBlog(Long id) {
//        //1.获取登录用户
//        UserDTO user = UserHolder.getUser();
//        //2.判读登录用户是否点赞
//        String key = BLOG_LIKED_KEY+user.getId();
//        Boolean isMember = stringRedisTemplate.opsForSet().isMember(key,user.getId().toString());
//        //3.若未点赞，可以点赞
//        if(!isMember){
//            //3.1 数据库点赞数+1
//            this.update().setSql("liked = liked + 1").eq("id",id).update();
//            //3.2 保存用户到redis的set集合中
//            stringRedisTemplate.opsForSet().add(key,user.getId().toString());
//        }else{
//            //4.若已点赞，取消点赞
//            //4.1 数据库点赞数-1
//            this.update().setSql("liked = liked - 1").eq("id",id).update();
//            stringRedisTemplate.opsForSet().remove(key,user.getId().toString());
//            //4.2 清除redis的set集合里的userId
//        }
//        return Result.ok();
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + id;
        Double score = stringRedisTemplate.opsForZSet().score(key, userId.toString());
        if(score==null){
            boolean success = update().setSql("liked = liked + 1").eq("id", id).update();
            if(success){
                stringRedisTemplate.opsForZSet().add(key,userId.toString(),System.currentTimeMillis());
            }
        }else {
            boolean success = update().setSql("liked = liked - 1").eq("id", id).update();
            if (success){
                stringRedisTemplate.opsForZSet().remove(key,userId.toString());
            }
        }
        return Result.ok();
    }

    @Override
    public Result BlogLikes(Long id) {
        String key = BLOG_LIKED_KEY + id;
        Set<String> top5 = stringRedisTemplate.opsForZSet().range(key,0,4);
        if(top5==null){
            return Result.ok(Collections.emptyList());
        }
        List<Long> ids = top5.stream().map(Long::valueOf).collect(Collectors.toList());
        String join = StrUtil.join(",",ids);
        List<UserDTO> userDTOS = userService.query()
                .in("id", ids).last("order by field(id," + join + ")").list()
                .stream()
                .map(user -> BeanUtil.copyProperties(user, UserDTO.class))
                .collect(Collectors.toList());
        return Result.ok(userDTOS);
    }

    public void isBlogLinked(Blog blog){
        Long userId = UserHolder.getUser().getId();
        String key = BLOG_LIKED_KEY + blog.getId();
        Double score = stringRedisTemplate.opsForZSet().score(key,userId.toString());
        if(score!=null){
            blog.setIsLike(true);
        }else {
            blog.setIsLike(false);
        }
    }
}
