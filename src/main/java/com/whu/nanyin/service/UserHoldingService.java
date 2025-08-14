package com.whu.nanyin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.FundTransaction;
import java.util.List;
import com.whu.nanyin.pojo.vo.UserHoldingVO;


public interface UserHoldingService extends IService<UserHolding> {

    // 用于根据客户ID查询其所有持仓
    List<UserHolding> listByuserId(Long userId);


    // 处理一笔新交易并自动更新持仓【交易发生后自动实现 但目前没有在系统里提供进行交易的前端接口 目前只用于批量导入交易数据时自动更新持仓数据】
    void updateHoldingAfterNewTransaction(FundTransaction transaction);



    /**
     * 获取单个客户市值排名前N的持仓详情
     * @param userId 客户ID
     * @param limit 要获取的记录数
     * @return 包含持仓视图对象(VO)的列表
     */
    List<UserHoldingVO> getTopNHoldings(Long userId, int limit);






}