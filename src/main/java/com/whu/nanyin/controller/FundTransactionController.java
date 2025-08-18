package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.dto.FundPurchaseDTO;
import com.whu.nanyin.pojo.dto.FundRedeemDTO;
import com.whu.nanyin.pojo.entity.FundTransaction;
import com.whu.nanyin.pojo.vo.ApiResponseVO;
import com.whu.nanyin.mapper.UserMapper;
import com.whu.nanyin.pojo.entity.User;
import com.whu.nanyin.pojo.vo.FundTransactionVO;
import com.whu.nanyin.security.CustomUserDetails;
import com.whu.nanyin.service.FundTransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 基金交易控制器
 * 
 * 提供基金交易相关的REST API接口，包括：
 * 1. 基金申购功能
 * 2. 基金赎回功能
 * 3. 交易记录查询功能
 * 4. 单条交易详情查询功能
 * 
 * 所有接口都需要用户认证，通过JWT token获取当前登录用户信息
 * 使用统一的ApiResponseVO格式返回响应数据
 */
@RestController
@RequestMapping("/api/transaction") // 基金交易相关接口的统一路径前缀
@Tag(name = "个人基金交易", description = "提供个人基金交易的申购、赎回与查询接口")
public class FundTransactionController {

    /**
     * 基金交易服务层接口
     * 负责处理基金申购、赎回等业务逻辑
     */
    @Autowired
    private FundTransactionService fundTransactionService;

    /**
     * 用户数据访问层接口
     * 用于查询用户信息，如账户余额等
     */
    @Autowired
    private UserMapper userMapper;

    /**
     * 申购基金接口
     * 
     * 用户通过此接口申购指定基金，系统会：
     * 1. 验证用户身份和申购参数
     * 2. 检查用户账户余额是否充足
     * 3. 创建申购交易记录
     * 4. 扣除用户账户资金
     * 5. 返回交易结果和最新账户余额
     * 
     * @param dto 基金申购数据传输对象，包含基金代码、申购金额等信息
     * @param authentication Spring Security认证对象，包含当前登录用户信息
     * @return ResponseEntity包装的ApiResponseVO，成功时返回交易详情，失败时返回错误信息
     */
    @Operation(summary = "申购基金")
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponseVO<FundTransactionVO>> purchase(@RequestBody @Validated FundPurchaseDTO dto, Authentication authentication) {
        try {
            // 从认证对象中获取当前登录用户的详细信息
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long currentUserId = userDetails.getId();
            
            // 设置申购用户ID，确保数据安全性
            dto.setUserId(currentUserId);
            
            // 调用服务层创建申购交易
            FundTransaction entity = fundTransactionService.createPurchaseTransaction(dto);

            // 将实体对象转换为视图对象
            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());
            
            // 查询最新余额，用于回传给前端直接展示
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null) {
                vo.setAvailableBalance(currentUser.getBalance());
            }

            return ResponseEntity.ok(ApiResponseVO.success("申购成功", vo));
        } catch (Exception e) {
            // 捕获异常并返回友好的错误信息
            return ResponseEntity.badRequest().body(ApiResponseVO.error("申购失败: " + e.getMessage()));
        }
    }

    /**
     * 赎回基金接口
     * 
     * 用户通过此接口赎回持有的基金份额，系统会：
     * 1. 验证用户身份和赎回参数
     * 2. 检查用户持有的基金份额是否充足
     * 3. 创建赎回交易记录
     * 4. 减少用户基金持有份额
     * 5. 增加用户账户资金
     * 6. 返回交易结果
     * 
     * @param dto 基金赎回数据传输对象，包含基金代码、赎回份额等信息
     * @param authentication Spring Security认证对象，包含当前登录用户信息
     * @return ResponseEntity包装的ApiResponseVO，成功时返回交易详情，失败时返回错误信息
     */
    @Operation(summary = "赎回基金")
    @PostMapping("/redeem")
    public ResponseEntity<ApiResponseVO<FundTransactionVO>> redeem(@RequestBody @Validated FundRedeemDTO dto, Authentication authentication) {
        try {
            // 从认证对象中获取当前登录用户的详细信息
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long currentUserId = userDetails.getId();
            
            // 设置赎回用户ID，确保数据安全性
            dto.setUserId(currentUserId);
            
            // 调用服务层创建赎回交易
            FundTransaction entity = fundTransactionService.createRedeemTransaction(dto);

            // 将实体对象转换为视图对象
            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());

            return ResponseEntity.ok(ApiResponseVO.success("赎回成功", vo));
        } catch (Exception e) {
            // 捕获异常并返回友好的错误信息
            return ResponseEntity.badRequest().body(ApiResponseVO.error("赎回失败: " + e.getMessage()));
        }
    }

    /**
     * 查询当前登录用户的所有交易记录接口
     * 
     * 获取当前登录用户的所有基金交易记录，包括申购和赎回记录
     * 返回的交易记录包含基金名称等详细信息，方便前端展示
     * 
     * @param authentication Spring Security认证对象，包含当前登录用户信息
     * @return ResponseEntity包装的Map，包含用户的所有交易记录列表
     */
    @Operation(summary = "查询【当前登录用户】的所有交易记录")
    @GetMapping("/my-transactions")
    public ResponseEntity<Map<String, Object>> getMyTransactions(Authentication authentication) {
        // 从认证对象中获取当前登录用户的详细信息
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        
        // 使用新方法获取包含基金名称的交易记录
        List<FundTransactionVO> transactionVOs = fundTransactionService.listByUserIdWithFundName(currentUserId);
    
        // 构建响应数据
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactionVOs);
        return ResponseEntity.ok(response);
    }

    /**
     * 根据交易ID查询当前用户的单条交易详情接口
     * 
     * 根据交易ID获取当前登录用户的特定交易记录详情
     * 系统会验证交易记录是否属于当前用户，确保数据安全性
     * 返回的交易详情包含基金名称等完整信息
     * 
     * @param id 交易记录的唯一标识ID
     * @param authentication Spring Security认证对象，包含当前登录用户信息
     * @return ResponseEntity包装的ApiResponseVO，成功时返回交易详情，失败时返回错误信息
     */
    @Operation(summary = "根据交易ID查询【当前用户】的单条交易详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseVO<FundTransactionVO>> getById(@PathVariable Long id, Authentication authentication) {
        // 从认证对象中获取当前登录用户的详细信息
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        try {
            // 使用新方法获取包含基金名称的交易详情
            // 服务层会验证交易记录是否属于当前用户
            FundTransactionVO vo = fundTransactionService.getTransactionByIdAndUserIdWithFundName(id, currentUserId);

            return ResponseEntity.ok(ApiResponseVO.success("交易详情获取成功", vo));
        } catch (Exception e) {
            // Service层在找不到或无码表示禁止访问权访问时会抛出异常
            // 返回403状态
            return ResponseEntity.status(403).body(ApiResponseVO.error(e.getMessage()));
        }
    }
}