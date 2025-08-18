package com.whu.nanyin.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.FundTransaction;
import java.util.List;
import com.whu.nanyin.pojo.vo.UserHoldingVO;

/**
 * @description 用户持仓服务接口，定义了与用户持仓相关的业务逻辑方法。
 * 继承了MyBatis-Plus的IService接口，从而获得基础的CRUD能力。
 */
public interface UserHoldingService extends IService<UserHolding> {

    /**
     * @description 根据用户ID查询其所有的持仓记录。
     * @param userId 用户的唯一ID。
     * @return 返回该用户的所有持仓实体对象列表。
     */
    List<UserHolding> listByuserId(Long userId);

    /**
     * @description 根据用户ID和可选的基金代码或名称，筛选查询持仓记录。
     * @param userId   用户的唯一ID。
     * @param fundCode 基金代码 (可选的筛选条件)。
     * @param fundName 基金名称 (可选的筛选条件)。
     * @return 返回符合条件的持仓实体对象列表。
     */
    List<UserHolding> listByUserIdAndFundInfo(Long userId, String fundCode, String fundName);


    /**
     * @description 在一笔新的基金交易（申购/赎回）发生后，调用此方法来更新用户的持仓信息。
     * 此方法主要用于在交易数据批量导入或实时交易发生后，自动维护持仓数据的准确性。
     * 目前在系统中，主要在后端处理交易时被调用。
     * @param transaction 新发生的交易记录实体。
     */
    void updateHoldingAfterNewTransaction(FundTransaction transaction);



    /**
     * @description 获取指定客户的、按市值排名的前N条持仓记录详情。
     * @param userId 客户的唯一ID。
     * @param limit  要获取的记录数量。
     * @return 返回一个列表，其中包含了封装好的持仓视图对象(VO)，用于前端展示。
     */
    List<UserHoldingVO> getTopNHoldings(Long userId, int limit);

}