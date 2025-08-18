package com.whu.nanyin.pojo.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 基金交易实体类
 * 
 * <p>该实体类用于表示基金交易记录，包括基金申购、赎回等各种交易类型。
 * 每条记录代表用户的一次基金交易操作，记录了交易的完整信息。</p>
 * 
 * <h3>主要功能：</h3>
 * <ul>
 *   <li><strong>交易记录存储</strong>：持久化保存用户的基金交易信息</li>
 *   <li><strong>交易历史追踪</strong>：支持查询用户的历史交易记录</li>
 *   <li><strong>业务数据支撑</strong>：为基金交易业务提供数据模型支持</li>
 * </ul>
 * 
 * <h3>交易类型包括：</h3>
 * <ul>
 *   <li>申购（PURCHASE）：用户购买基金份额</li>
 *   <li>赎回（REDEEM）：用户卖出基金份额</li>
 * </ul>
 * 
 * <h3>数据库映射：</h3>
 * <p>该实体对应数据库表：user_transactions</p>
 */
@Data
@TableName("user_transactions")
public class FundTransaction {

    /**
     * 交易记录主键ID
     * 
     * <p>数据库自增主键，唯一标识每一条交易记录。
     * 该字段由数据库自动生成，无需手动设置。</p>
     * 
     * <p><strong>特点：</strong></p>
     * <ul>
     *   <li>自增长：每次插入新记录时自动递增</li>
     *   <li>唯一性：确保每条交易记录都有唯一标识</li>
     *   <li>不可变：一旦生成不应被修改</li>
     * </ul>
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     * 
     * <p>标识发起交易的用户身份，关联到用户表的主键。
     * 用于确定交易记录的归属关系。</p>
     * 
     * <p><strong>业务用途：</strong></p>
     * <ul>
     *   <li>用户身份验证：确保用户只能查看自己的交易记录</li>
     *   <li>数据隔离：实现多用户数据的安全隔离</li>
     *   <li>关联查询：支持用户维度的交易统计和分析</li>
     * </ul>
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 基金代码
     * 
     * <p>标识交易涉及的基金产品，通常为6位数字代码。
     * 用于关联基金基本信息和净值数据。</p>
     * 
     * <p><strong>示例：</strong></p>
     * <ul>
     *   <li>000001：华夏成长混合</li>
     *   <li>110022：易方达消费行业股票</li>
     *   <li>161725：招商中证白酒指数分级</li>
     * </ul>
     * 
     * <p><strong>业务用途：</strong></p>
     * <ul>
     *   <li>产品识别：准确定位交易的基金产品</li>
     *   <li>净值查询：获取交易时点的基金净值</li>
     *   <li>产品分析：支持按基金维度的交易统计</li>
     * </ul>
     */
    @TableField("fund_code")
    private String fundCode;
    
    /**
     * 交易类型
     * 
     * <p>标识本次交易的操作类型，决定了交易的业务逻辑。</p>
     * 
     * <p><strong>支持的交易类型：</strong></p>
     * <ul>
     *   <li><strong>PURCHASE</strong>：申购，用户购买基金份额</li>
     *   <li><strong>REDEEM</strong>：赎回，用户卖出基金份额</li>
     * </ul>
     * 
     * <p><strong>业务影响：</strong></p>
     * <ul>
     *   <li>申购：增加用户基金份额，减少银行账户余额</li>
     *   <li>赎回：减少用户基金份额，增加银行账户余额</li>
     * </ul>
     */
    @TableField("transaction_type")
    private String transactionType;
    
    /**
     * 交易金额
     * 
     * <p>本次交易涉及的资金数额，以人民币为单位。
     * 对于申购交易，表示投入的资金；对于赎回交易，表示获得的资金。</p>
     * 
     * <p><strong>精度要求：</strong></p>
     * <ul>
     *   <li>小数位数：通常保留2位小数（精确到分）</li>
     *   <li>数据类型：使用BigDecimal确保精度</li>
     *   <li>取值范围：必须为正数</li>
     * </ul>
     * 
     * <p><strong>计算逻辑：</strong></p>
     * <ul>
     *   <li>申购：交易金额 = 申购金额 + 手续费</li>
     *   <li>赎回：交易金额 = 赎回金额 - 手续费</li>
     * </ul>
     */
    @TableField("transaction_amount")
    private BigDecimal transactionAmount;
    
    /**
     * 交易份额
     * 
     * <p>本次交易涉及的基金份额数量。
     * 对于申购交易，表示获得的份额；对于赎回交易，表示卖出的份额。</p>
     * 
     * <p><strong>计算公式：</strong></p>
     * <ul>
     *   <li>申购份额 = (申购金额 - 手续费) / 基金净值</li>
     *   <li>赎回金额 = 赎回份额 × 基金净值 - 手续费</li>
     * </ul>
     * 
     * <p><strong>精度要求：</strong></p>
     * <ul>
     *   <li>小数位数：通常保留4位小数</li>
     *   <li>数据类型：使用BigDecimal确保计算精度</li>
     *   <li>取值范围：必须为正数</li>
     * </ul>
     */
    @TableField("transaction_shares")
    private BigDecimal transactionShares;
    
    /**
     * 基金单位净值
     * 
     * <p>交易执行时的基金单位净值，用于计算交易份额和金额。
     * 该净值通常为交易确认日的基金净值。</p>
     * 
     * <p><strong>业务意义：</strong></p>
     * <ul>
     *   <li>价格基准：作为份额和金额转换的基础</li>
     *   <li>历史记录：保存交易时点的净值信息</li>
     *   <li>收益计算：用于计算投资收益和回报率</li>
     * </ul>
     * 
     * <p><strong>数据来源：</strong></p>
     * <ul>
     *   <li>基金公司公布的官方净值</li>
     *   <li>通常在交易日结束后更新</li>
     *   <li>节假日和周末不更新</li>
     * </ul>
     */
    @TableField("share_price")
    private BigDecimal sharePrice;
    
    /**
     * 交易时间
     * 
     * <p>用户发起交易请求的时间，记录交易的具体时刻。
     * 该时间用于交易排序、统计分析和监管报告。</p>
     * 
     * <p><strong>时间特点：</strong></p>
     * <ul>
     *   <li>格式：yyyy-MM-dd HH:mm:ss</li>
     *   <li>时区：使用系统默认时区</li>
     *   <li>精度：精确到秒</li>
     * </ul>
     * 
     * <p><strong>业务用途：</strong></p>
     * <ul>
     *   <li>交易确认：确定交易的有效性和优先级</li>
     *   <li>历史查询：支持按时间范围查询交易记录</li>
     *   <li>统计分析：用于交易量和趋势分析</li>
     * </ul>
     */
    @TableField("transaction_time")
    private LocalDateTime transactionTime;
    
    /**
     * 交易状态
     * 
     * <p>标识交易当前的处理状态，反映交易的生命周期。</p>
     * 
     * <p><strong>状态类型：</strong></p>
     * <ul>
     *   <li><strong>PENDING</strong>：待处理，交易已提交但未确认</li>
     *   <li><strong>CONFIRMED</strong>：已确认，交易成功完成</li>
     *   <li><strong>FAILED</strong>：失败，交易因各种原因未能完成</li>
     *   <li><strong>CANCELLED</strong>：已取消，用户主动取消交易</li>
     * </ul>
     * 
     * <p><strong>状态流转：</strong></p>
     * <ol>
     *   <li>PENDING → CONFIRMED：交易成功确认</li>
     *   <li>PENDING → FAILED：交易失败（余额不足、系统错误等）</li>
     *   <li>PENDING → CANCELLED：用户取消交易</li>
     * </ol>
     */
    @TableField("status")
    private String status;

    /**
     * 银行账户号码
     * 
     * <p>与本次交易关联的银行卡号，用于资金的扣款或到账。
     * 该卡号必须是用户在系统中已绑定的有效银行账户。</p>
     * 
     * <p><strong>安全要求：</strong></p>
     * <ul>
     *   <li>身份验证：确保卡号属于当前交易用户</li>
     *   <li>状态检查：验证银行卡是否为正常状态</li>
     *   <li>余额验证：申购时检查账户余额是否充足</li>
     * </ul>
     * 
     * <p><strong>业务用途：</strong></p>
     * <ul>
     *   <li>资金扣款：申购时从该账户扣除资金</li>
     *   <li>资金到账：赎回时向该账户转入资金</li>
     *   <li>交易追溯：记录资金流向以便审计</li>
     * </ul>
     */
    @TableField("bank_account_number")
    private String bankAccountNumber;

    /**
     * 记录创建时间
     * 
     * <p>数据库记录的创建时间，由MyBatis-Plus自动填充。
     * 该字段在插入记录时自动设置为当前时间。</p>
     * 
     * <p><strong>自动填充特性：</strong></p>
     * <ul>
     *   <li>插入时填充：新增记录时自动设置</li>
     *   <li>不可修改：一旦设置不会被更新</li>
     *   <li>审计用途：用于数据审计和问题追踪</li>
     * </ul>
     */
    @TableField(value = "create_time", fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /**
     * 记录更新时间
     * 
     * <p>数据库记录的最后更新时间，由MyBatis-Plus自动维护。
     * 该字段在插入和更新记录时都会自动设置为当前时间。</p>
     * 
     * <p><strong>自动填充特性：</strong></p>
     * <ul>
     *   <li>插入时填充：新增记录时自动设置</li>
     *   <li>更新时填充：修改记录时自动更新</li>
     *   <li>变更追踪：用于追踪数据的最后修改时间</li>
     * </ul>
     */
    @TableField(value = "update_time", fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}