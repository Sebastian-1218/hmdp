package com.hmdp.service.impl;

import com.hmdp.dto.Result;
import com.hmdp.entity.SeckillVoucher;
import com.hmdp.entity.Voucher;
import com.hmdp.entity.VoucherOrder;
import com.hmdp.mapper.SeckillVoucherMapper;
import com.hmdp.mapper.VoucherMapper;
import com.hmdp.mapper.VoucherOrderMapper;
import com.hmdp.service.IVoucherOrderService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.hmdp.utils.RedisIdWorker;
import com.hmdp.utils.UserHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;

/**
 * <p>
 *  服务实现类
 * </p>
 *
 * @author 虎哥
 * @since 2021-12-22
 */
@Service
public class VoucherOrderServiceImpl extends ServiceImpl<VoucherOrderMapper, VoucherOrder> implements IVoucherOrderService {
    @Resource
    private VoucherMapper voucherMapper;
    @Resource
    private SeckillVoucherMapper seckillVoucherMapper;
    @Resource
    private RedisIdWorker redisIdWorker;
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Boolean test(Long voucherId) throws Exception {
       SeckillVoucher voucher = voucherMapper.selectStock(voucherId);
       long startTime = voucher.getBeginTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        long endTime = voucher.getEndTime().toInstant(ZoneOffset.of("+8")).toEpochMilli();
        int stock = voucher.getStock();
        if(System.currentTimeMillis()<endTime&&System.currentTimeMillis()>startTime){
            if(stock<0){
                throw new Exception("已被抢完");
            }
        }else {
            throw new Exception("未开始或已结束活动");
        }

//        seckillVoucherMapper.updateById(voucher);
        Boolean isUpdate = seckillVoucherMapper.updateAA(voucher.getVoucherId(),voucher.getStock());
        VoucherOrder voucherOrder = new VoucherOrder();
        long Id = redisIdWorker.nextId("order");
        voucherOrder.setId(Id);
        voucherOrder.setPayType(1);
        voucherOrder.setUserId(UserHolder.getUser().getId());
        voucherOrder.setVoucherId(voucherId);
        voucherOrder.setStatus(1);
        return save(voucherOrder) ?true:false;
    }
}
