package com.whu.nanyin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whu.nanyin.exception.InsufficientFundsException;
import com.whu.nanyin.mapper.FundTransactionMapper;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.dto.FundPurchaseDTO;
import com.whu.nanyin.pojo.dto.FundRedeemDTO;
import com.whu.nanyin.pojo.entity.User;
import com.whu.nanyin.pojo.entity.UserHolding;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.vo.FundDetailVO;
import com.whu.nanyin.pojo.vo.FundTransactionVO;
import com.whu.nanyin.service.*;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;

/**
 * 基金交易服务实现类
 * 
 * <p>该类是基金交易业务的核心实现，负责处理基金的申购和赎回操作。</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *   <li><strong>基金申购</strong>：处理用户购买基金的完整流程，包括余额校验、净值计算、份额分配</li>
 *   <li><strong>基金赎回</strong>：处理用户卖出基金的完整流程，包括持仓校验、金额计算、资金返还</li>
 *   <li><strong>交易查询</strong>：提供多种维度的交易记录查询功能</li>
 *   <li><strong>数据一致性</strong>：确保交易记录与用户持仓数据的实时同步</li>
 * </ul>
 * 
 * <h3>业务特点：</h3>
 * <ul>
 *   <li><strong>事务管理</strong>：关键操作使用@Transactional确保数据一致性</li>
 *   <li><strong>精度控制</strong>：使用BigDecimal进行金融计算，避免精度丢失</li>
 *   <li><strong>安全校验</strong>：严格的权限控制和数据校验机制</li>
 *   <li><strong>实时净值</strong>：基于最新基金净值进行交易计算</li>
 * </ul>
 * 
 * <h3>依赖关系：</h3>
 * <ul>
 *   <li>继承MyBatis-Plus的ServiceImpl，提供基础CRUD功能</li>
 *   <li>依赖FundInfoService获取基金信息和净值数据</li>
 *   <li>依赖UserHoldingService管理用户持仓信息</li>
 *   <li>依赖UserMapper进行用户账户操作</li>
 * </ul>
 * 
 * @author 系统开发团队
 * @version 1.0
 * @since 2024
 * @see FundTransactionService
 * @see FundTransaction
 * @see FundPurchaseDTO
 * @see FundRedeemDTO
 */
@Service
@Slf4j
public class FundTransactionServiceImpl extends ServiceImpl<FundTransactionMapper, FundTransaction> implements FundTransactionService {

    /**
     * 基金信息服务
     * 
     * <p>用于获取基金的详细信息，包括：</p>
     * <ul>
     *   <li>基金基本信息（名称、代码等）</li>
     *   <li>基金业绩数据（净值、收益率等）</li>
     *   <li>基金状态信息（是否可交易等）</li>
     * </ul>
     * 
     * <p><strong>业务用途：</strong></p>
     * <ul>
     *   <li>申购时获取最新净值计算份额</li>
     *   <li>赎回时获取最新净值计算金额</li>
     *   <li>交易查询时补充基金名称信息</li>
     * </ul>
     */
    @Autowired
    private FundInfoService fundInfoService;

    /**
     * 用户持仓服务（懒加载）
     * 
     * <p>使用@Lazy注解解决循环依赖问题，因为UserHoldingService可能也依赖FundTransactionService。</p>
     * 
     * <p><strong>主要功能：</strong></p>
     * <ul>
     *   <li>查询用户当前持仓份额（赎回时校验）</li>
     *   <li>更新用户持仓信息（交易完成后同步）</li>
     *   <li>计算持仓成本和收益（业务分析）</li>
     * </ul>
     * 
     * <p><strong>循环依赖说明：</strong></p>
     * <ul>
     *   <li>FundTransactionService需要UserHoldingService更新持仓</li>
     *   <li>UserHoldingService可能需要FundTransactionService查询交易历史</li>
     *   <li>@Lazy确保在实际使用时才初始化，避免启动时的循环依赖</li>
     * </ul>
     */
    @Autowired
    @Lazy
    private UserHoldingService userHoldingService;

    /**
     * 用户数据访问对象
     * 
     * <p>直接操作用户表，主要用于账户余额管理：</p>
     * <ul>
     *   <li><strong>申购时</strong>：扣除用户账户余额</li>
     *   <li><strong>赎回时</strong>：增加用户账户余额</li>
     *   <li><strong>校验时</strong>：检查用户余额是否充足</li>
     * </ul>
     * 
     * <p><strong>数据安全：</strong></p>
     * <ul>
     *   <li>所有余额操作都在事务内执行</li>
     *   <li>使用BigDecimal确保金额计算精度</li>
     *   <li>操作前进行充分的数据校验</li>
     * </ul>
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 创建基金申购交易
     * 
     * <p>处理用户购买基金的完整业务流程，确保数据一致性和业务正确性。</p>
     * 
     * <h3>业务流程：</h3>
     * <ol>
     *   <li><strong>用户校验</strong>：验证用户存在性和账户余额充足性</li>
     *   <li><strong>余额扣除</strong>：从用户账户扣除申购金额</li>
     *   <li><strong>基金校验</strong>：验证基金存在性和可交易性</li>
     *   <li><strong>净值获取</strong>：获取基金最新单位净值</li>
     *   <li><strong>份额计算</strong>：根据申购金额和净值计算可获得份额</li>
     *   <li><strong>交易记录</strong>：创建并保存交易记录</li>
     *   <li><strong>持仓更新</strong>：同步更新用户持仓信息</li>
     * </ol>
     * 
     * <h3>计算公式：</h3>
     * <pre>
     * 申购份额 = 申购金额 ÷ 单位净值
     * 精度：保留4位小数，使用四舍五入
     * </pre>
     * 
     * <h3>异常处理：</h3>
     * <ul>
     *   <li><strong>InsufficientFundsException</strong>：余额不足时抛出</li>
     *   <li><strong>IllegalArgumentException</strong>：基金信息无效时抛出</li>
     *   <li><strong>RuntimeException</strong>：数据保存失败时抛出</li>
     * </ul>
     * 
     * @param dto 申购请求数据传输对象，包含用户ID、基金代码、申购金额、交易时间、银行卡号等信息
     * @return 创建并保存的交易记录实体，包含数据库生成的交易ID和计算得出的份额信息
     * @throws InsufficientFundsException 当用户账户余额不足时抛出
     * @throws IllegalArgumentException 当基金信息无效或净值缺失时抛出
     * @throws RuntimeException 当交易记录保存失败时抛出
     * @see FundPurchaseDTO
     * @see FundTransaction
     * @see #saveTransactionAndUpdateHolding(FundTransaction)
     */
    @Override
    @Transactional
    public FundTransaction createPurchaseTransaction(FundPurchaseDTO dto) {
        // 1. 校验并扣除用户余额
        User user = userMapper.selectById(dto.getUserId());
        if (user == null || user.getBalance().compareTo(dto.getTransactionAmount()) < 0) {
            throw new InsufficientFundsException("购买失败：账户余额不足。");
        }
        user.setBalance(user.getBalance().subtract(dto.getTransactionAmount()));
        userMapper.updateById(user);

        // 2. 获取基金信息
        FundDetailVO fundDetail = fundInfoService.getFundDetail(dto.getFundCode());
        Assert.notNull(fundDetail, "找不到对应的基金信息：" + dto.getFundCode());
        Assert.notNull(fundDetail.getPerformance(), "该基金暂无有效的业绩信息，无法交易。");
        BigDecimal sharePrice = fundDetail.getPerformance().getUnitNetValue();
        Assert.notNull(sharePrice, "该基金暂无有效的净值信息，无法交易。");

        // 3. 创建交易实体并手动赋值
        FundTransaction transaction = new FundTransaction();
        transaction.setUserId(dto.getUserId());
        transaction.setFundCode(dto.getFundCode());
        transaction.setTransactionAmount(dto.getTransactionAmount());
        transaction.setTransactionTime(LocalDateTime.now());
        
        // 设置银行卡号（重要：确保数据完整性）
        transaction.setBankAccountNumber(dto.getBankAccountNumber());

        transaction.setTransactionType("申购");
        transaction.setSharePrice(sharePrice);
        transaction.setStatus("成功");

        // 计算申购份额：申购金额 ÷ 单位净值，保留4位小数
        BigDecimal shares = dto.getTransactionAmount().divide(sharePrice, 4, RoundingMode.HALF_UP);
        transaction.setTransactionShares(shares);

        // 4. 保存交易并更新持仓
        return saveTransactionAndUpdateHolding(transaction);
    }

    /**
     * 根据用户ID和基金代码查询最近的申购交易记录
     * 
     * <p>用于获取用户购买指定基金时使用的银行卡号，以便在赎回时使用相同的银行卡。</p>
     * 
     * @param userId 用户唯一标识ID
     * @param fundCode 基金代码
     * @return 最近的申购交易记录，如果没有找到则返回null
     */
    @Override
    public FundTransaction getLatestPurchaseTransaction(Long userId, String fundCode) {
        QueryWrapper<FundTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("fund_code", fundCode)
                   .eq("transaction_type", "申购")
                   .orderByDesc("create_time")
                   .last("LIMIT 1");
        return this.getOne(queryWrapper);
    }

    /**
     * 创建基金赎回交易
     * 
     * <p>处理用户卖出基金的完整业务流程，自动使用购买时的银行卡号进行赎回。</p>
     * 
     * <h3>业务流程：</h3>
     * <ol>
     *   <li><strong>持仓校验</strong>：验证用户持有足够的基金份额</li>
     *   <li><strong>银行卡查询</strong>：自动获取购买时使用的银行卡号</li>
     *   <li><strong>净值获取</strong>：获取基金最新单位净值</li>
     *   <li><strong>金额计算</strong>：根据赎回份额和净值计算可获得金额</li>
     *   <li><strong>资金返还</strong>：将赎回金额加入用户账户余额</li>
     *   <li><strong>交易记录</strong>：创建并保存交易记录</li>
     *   <li><strong>持仓更新</strong>：同步减少用户持仓份额</li>
     * </ol>
     * 
     * @param dto 赎回请求数据传输对象，包含用户ID、基金代码、赎回份额、交易时间等信息
     * @return 创建并保存的交易记录实体，包含数据库生成的交易ID和计算得出的赎回金额
     * @throws InsufficientFundsException 当用户持仓份额不足时抛出
     * @throws IllegalArgumentException 当基金信息无效或找不到购买记录时抛出
     */
    @Override
    @Transactional
    public FundTransaction createRedeemTransaction(FundRedeemDTO dto) {
        // 1. 校验用户持仓份额是否足够
        QueryWrapper<UserHolding> holdingQuery = new QueryWrapper<>();
        holdingQuery.eq("user_id", dto.getUserId()).eq("fund_code", dto.getFundCode());
        UserHolding currentHolding = userHoldingService.getOne(holdingQuery);
        if (currentHolding == null || dto.getTransactionShares().compareTo(currentHolding.getTotalShares()) > 0) {
            String availableShares = (currentHolding != null) ? currentHolding.getTotalShares().toPlainString() : "0";
            throw new InsufficientFundsException("赎回失败：份额不足。当前持有 " + availableShares + " 份，尝试赎回 " + dto.getTransactionShares().toPlainString() + " 份。");
        }

        // 2. 自动获取购买时使用的银行卡号
        FundTransaction latestPurchase = getLatestPurchaseTransaction(dto.getUserId(), dto.getFundCode());
        if (latestPurchase == null || latestPurchase.getBankAccountNumber() == null) {
            throw new IllegalArgumentException("赎回失败：找不到该基金的购买记录或银行卡信息。");
        }
        String bankAccountNumber = latestPurchase.getBankAccountNumber();

        // 3. 获取基金最新净值
        FundDetailVO fundDetail = fundInfoService.getFundDetail(dto.getFundCode());
        Assert.notNull(fundDetail, "找不到对应的基金信息：" + dto.getFundCode());
        Assert.notNull(fundDetail.getPerformance(), "该基金暂无有效的业绩信息，无法交易。");
        BigDecimal sharePrice = fundDetail.getPerformance().getUnitNetValue();
        Assert.notNull(sharePrice, "该基金暂无有效的净值信息，无法交易。");

        // 4. 计算赎回可获得的金额 (份额 × 净值)，保留2位小数
        BigDecimal redeemAmount = dto.getTransactionShares().multiply(sharePrice).setScale(2, RoundingMode.HALF_UP);

        // 5. 将赎回金额增加到用户的可用余额中
        User user = userMapper.selectById(dto.getUserId());
        user.setBalance(user.getBalance().add(redeemAmount));
        userMapper.updateById(user);

        // 6. 创建并保存交易记录实体
        FundTransaction transaction = new FundTransaction();
        transaction.setUserId(dto.getUserId());
        transaction.setFundCode(dto.getFundCode());
        transaction.setTransactionShares(dto.getTransactionShares());
        transaction.setTransactionTime(LocalDateTime.now()); // 使用服务器当前时间
        transaction.setBankAccountNumber(bankAccountNumber); // 使用购买时的银行卡号
        transaction.setTransactionType("赎回");
        transaction.setSharePrice(sharePrice);
        transaction.setStatus("成功");

        // 设置计算出的赎回金额（重要：确保金额准确性）
        transaction.setTransactionAmount(redeemAmount);

        // 7. 保存交易记录并触发持仓更新
        return saveTransactionAndUpdateHolding(transaction);
    }

    /**
     * 根据用户ID查询交易记录列表
     * 
     * <p>获取指定用户的所有基金交易记录，按交易ID升序排列。</p>
     * 
     * <h3>查询特点：</h3>
     * <ul>
     *   <li><strong>用户隔离</strong>：只返回指定用户的交易记录</li>
     *   <li><strong>顺序排列</strong>：按ID升序排序，确保时间顺序</li>
     *   <li><strong>完整信息</strong>：返回交易的所有字段信息</li>
     * </ul>
     * 
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>用户交易历史查询</li>
     *   <li>交易数据分析和统计</li>
     *   <li>内部业务逻辑处理</li>
     * </ul>
     * 
     * @param userId 用户唯一标识ID
     * @return 该用户的所有交易记录列表，按ID升序排列，如果用户无交易记录则返回空列表
     * @see FundTransaction
     */
    @Override  
    public List<FundTransaction> listByUserId(Long userId) {
        QueryWrapper<FundTransaction> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        queryWrapper.orderByAsc("id"); // 按ID升序排序，确保从1开始
        return this.list(queryWrapper);
    }

    /**
     * 根据用户ID查询交易记录列表（包含基金名称）
     * 
     * <p>获取指定用户的所有基金交易记录，并补充基金名称信息，用于前端展示。</p>
     * 
     * <h3>数据增强：</h3>
     * <ul>
     *   <li><strong>基金名称</strong>：通过基金代码查询并补充基金名称</li>
     *   <li><strong>VO转换</strong>：将实体对象转换为视图对象</li>
     *   <li><strong>用户友好</strong>：提供更直观的交易信息展示</li>
     * </ul>
     * 
     * <h3>处理逻辑：</h3>
     * <ol>
     *   <li>查询用户的所有交易记录</li>
     *   <li>遍历每条交易记录</li>
     *   <li>根据基金代码查询基金详细信息</li>
     *   <li>提取基金名称并设置到VO对象</li>
     *   <li>返回包含基金名称的VO列表</li>
     * </ol>
     * 
     * <h3>容错处理：</h3>
     * <ul>
     *   <li>如果基金信息查询失败，基金名称字段为空</li>
     *   <li>不影响其他交易记录的正常返回</li>
     * </ul>
     * 
     * @param userId 用户唯一标识ID
     * @return 包含基金名称的交易记录VO列表，如果用户无交易记录则返回空列表
     * @see FundTransactionVO
     * @see FundDetailVO
     * @see #listByUserId(Long)
     */
    @Override
    public List<FundTransactionVO> listByUserIdWithFundName(Long userId) {
        List<FundTransaction> transactions = listByUserId(userId);
        return transactions.stream().map(transaction -> {
            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(transaction, vo);
            
            // 根据fund_code查询基金名称
            FundDetailVO fundDetail = fundInfoService.getFundDetail(transaction.getFundCode());
            if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                vo.setFundName(fundDetail.getBasicInfo().getFundName());
            }
            
            return vo;
        }).collect(Collectors.toList());
    }

    /**
     * 保存交易记录并更新用户持仓
     * 
     * <p>这是一个私有辅助方法，用于统一处理交易保存和持仓更新的逻辑，确保数据一致性。</p>
     * 
     * <h3>执行步骤：</h3>
     * <ol>
     *   <li><strong>保存交易</strong>：将交易记录保存到数据库，获取自动生成的ID</li>
     *   <li><strong>校验结果</strong>：检查保存是否成功，ID是否正确生成</li>
     *   <li><strong>更新持仓</strong>：调用持仓服务，根据新交易更新用户持仓信息</li>
     *   <li><strong>返回结果</strong>：返回包含完整信息的交易实体</li>
     * </ol>
     * 
     * <h3>数据一致性保证：</h3>
     * <ul>
     *   <li><strong>原子操作</strong>：在同一事务内完成交易保存和持仓更新</li>
     *   <li><strong>失败回滚</strong>：任何步骤失败都会回滚整个事务</li>
     *   <li><strong>日志记录</strong>：记录关键操作的执行情况</li>
     * </ul>
     * 
     * <h3>异常处理：</h3>
     * <ul>
     *   <li>交易保存失败时抛出RuntimeException</li>
     *   <li>记录详细的错误日志便于问题排查</li>
     * </ul>
     * 
     * @param transaction 已经构建好的交易实体，包含所有必要的交易信息
     * @return 保存后的交易实体，包含数据库生成的ID和完整的交易信息
     * @throws RuntimeException 当交易保存失败时抛出
     * @see UserHoldingService#updateHoldingAfterNewTransaction(FundTransaction)
     */
    private FundTransaction saveTransactionAndUpdateHolding(FundTransaction transaction) {
        // 步骤1：将交易记录保存到数据库
        boolean ok = this.save(transaction);
        if (!ok || transaction.getId() == null) {
            log.error("[Transaction] Save transaction failed, tx={}", transaction);
            throw new RuntimeException("保存交易失败");
        }
        // 步骤2：调用客户持仓服务，根据这笔新交易实时更新持仓信息
        userHoldingService.updateHoldingAfterNewTransaction(transaction);
        // 步骤3：返回包含ID的完整交易实体
        return transaction;
    }

    /**
     * 安全获取指定用户的交易记录
     * 
     * <p>根据交易ID和用户ID获取交易记录，确保用户只能访问自己的交易数据。</p>
     * 
     * <h3>安全机制：</h3>
     * <ul>
     *   <li><strong>权限校验</strong>：验证交易记录是否属于指定用户</li>
     *   <li><strong>数据隔离</strong>：防止用户访问他人的交易信息</li>
     *   <li><strong>异常处理</strong>：无权访问时抛出访问拒绝异常</li>
     * </ul>
     * 
     * <h3>校验逻辑：</h3>
     * <ol>
     *   <li>根据交易ID查询交易记录</li>
     *   <li>检查交易记录是否存在</li>
     *   <li>验证交易记录的用户ID是否匹配</li>
     *   <li>匹配成功返回交易记录，否则抛出异常</li>
     * </ol>
     * 
     * <h3>使用场景：</h3>
     * <ul>
     *   <li>用户查看特定交易详情</li>
     *   <li>交易相关的业务操作</li>
     *   <li>需要权限控制的交易查询</li>
     * </ul>
     * 
     * @param transactionId 交易记录的唯一标识ID
     * @param userId 用户唯一标识ID，用于权限校验
     * @return 指定的交易记录实体
     * @throws AccessDeniedException 当交易不存在或用户无权访问时抛出，Spring Security会捕获并返回403 Forbidden
     * @see FundTransaction
     * @see AccessDeniedException
     */
    @Override
    public FundTransaction getTransactionByIdAndUserId(Long transactionId, Long userId) {
        FundTransaction transaction = this.getById(transactionId);

        // 安全校验：如果交易不存在，或者交易的userId与当前登录的userId不匹配
        if (transaction == null || !transaction.getUserId().equals(userId)) {
            // 抛出异常，Spring Security会捕获并返回403 Forbidden错误
            throw new AccessDeniedException("无权访问此交易记录");
        }

        return transaction;
    }

    /**
     * 安全获取指定用户的交易记录（包含基金名称）
     * 
     * <p>根据交易ID和用户ID获取交易记录，并补充基金名称信息，用于前端详情展示。</p>
     * 
     * <h3>功能特点：</h3>
     * <ul>
     *   <li><strong>安全访问</strong>：继承安全校验机制，确保数据访问权限</li>
     *   <li><strong>信息增强</strong>：补充基金名称，提供更友好的展示</li>
     *   <li><strong>VO转换</strong>：转换为视图对象，适合前端使用</li>
     * </ul>
     * 
     * <h3>处理流程：</h3>
     * <ol>
     *   <li>调用安全获取方法，确保权限校验</li>
     *   <li>将交易实体转换为VO对象</li>
     *   <li>根据基金代码查询基金信息</li>
     *   <li>提取并设置基金名称</li>
     *   <li>返回包含完整信息的VO对象</li>
     * </ol>
     * 
     * <h3>容错处理：</h3>
     * <ul>
     *   <li>如果基金信息查询失败，基金名称字段为空</li>
     *   <li>不影响交易记录本身的正常返回</li>
     * </ul>
     * 
     * @param transactionId 交易记录的唯一标识ID
     * @param userId 用户唯一标识ID，用于权限校验
     * @return 包含基金名称的交易记录VO对象
     * @throws AccessDeniedException 当交易不存在或用户无权访问时抛出
     * @see FundTransactionVO
     * @see #getTransactionByIdAndUserId(Long, Long)
     */
    @Override
    public FundTransactionVO getTransactionByIdAndUserIdWithFundName(Long transactionId, Long userId) {
        FundTransaction transaction = getTransactionByIdAndUserId(transactionId, userId);
        
        FundTransactionVO vo = new FundTransactionVO();
        BeanUtils.copyProperties(transaction, vo);
        
        // 根据fund_code查询基金名称
        FundDetailVO fundDetail = fundInfoService.getFundDetail(transaction.getFundCode());
        if (fundDetail != null && fundDetail.getBasicInfo() != null) {
            vo.setFundName(fundDetail.getBasicInfo().getFundName());
        }
        
        return vo;
    }

}