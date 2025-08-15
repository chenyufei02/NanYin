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
import com.whu.nanyin.service.FundInfoService;
import com.whu.nanyin.pojo.vo.FundDetailVO;
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
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/transaction")
@Tag(name = "个人基金交易", description = "提供个人基金交易的申购、赎回与查询接口")
public class FundTransactionController {

    @Autowired
    private FundTransactionService fundTransactionService;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    private FundInfoService fundInfoService;

    @Operation(summary = "申购基金")
    @PostMapping("/purchase")
    public ResponseEntity<ApiResponseVO<FundTransactionVO>> purchase(@RequestBody @Validated FundPurchaseDTO dto, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long currentUserId = userDetails.getId();
            dto.setUserId(currentUserId);
            FundTransaction entity = fundTransactionService.createPurchaseTransaction(dto);

            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());
            // 根据基金代码查询基金名称
            try {
                FundDetailVO fundDetail = fundInfoService.getFundDetail(entity.getFundCode());
                if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                    vo.setFundName(fundDetail.getBasicInfo().getFundName());
                }
            } catch (Exception e) {
                // 如果查询基金信息失败，不影响交易记录的返回，只是基金名称为空
            }
            // 查询最新余额，用于回传给前端直接展示
            User currentUser = userMapper.selectById(currentUserId);
            if (currentUser != null) {
                vo.setAvailableBalance(currentUser.getBalance());
            }

            return ResponseEntity.ok(ApiResponseVO.success("申购成功", vo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseVO.error("申购失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "赎回基金")
    @PostMapping("/redeem")
    public ResponseEntity<ApiResponseVO<FundTransactionVO>> redeem(@RequestBody @Validated FundRedeemDTO dto, Authentication authentication) {
        try {
            CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
            Long currentUserId = userDetails.getId();
            dto.setUserId(currentUserId);
            FundTransaction entity = fundTransactionService.createRedeemTransaction(dto);

            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());
            // 根据基金代码查询基金名称
            try {
                FundDetailVO fundDetail = fundInfoService.getFundDetail(entity.getFundCode());
                if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                    vo.setFundName(fundDetail.getBasicInfo().getFundName());
                }
            } catch (Exception e) {
                // 如果查询基金信息失败，不影响交易记录的返回，只是基金名称为空
            }

            return ResponseEntity.ok(ApiResponseVO.success("赎回成功", vo));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(ApiResponseVO.error("赎回失败: " + e.getMessage()));
        }
    }

    @Operation(summary = "查询【当前登录用户】的所有交易记录")
    @GetMapping("/my-transactions")
    public ResponseEntity<Map<String, Object>> getMyTransactions(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        List<FundTransaction> transactionEntities = fundTransactionService.listByUserId(currentUserId);
    
        List<FundTransactionVO> transactionVOs = transactionEntities.stream().map(entity -> {
            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());
            // 根据基金代码查询基金名称
            try {
                FundDetailVO fundDetail = fundInfoService.getFundDetail(entity.getFundCode());
                if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                    vo.setFundName(fundDetail.getBasicInfo().getFundName());
                }
            } catch (Exception e) {
                // 如果查询基金信息失败，不影响交易记录的返回，只是基金名称为空
            }
            return vo;
        }).collect(Collectors.toList());
    
        Map<String, Object> response = new HashMap<>();
        response.put("transactions", transactionVOs);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "查询【当前登录用户】的申购记录")
    @GetMapping("/my-purchases")
    public ResponseEntity<Map<String, Object>> getMyPurchases(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        List<FundTransaction> transactionEntities = fundTransactionService.listByUserIdAndTransactionType(currentUserId, "申购");
    
        List<FundTransactionVO> transactionVOs = transactionEntities.stream().map(entity -> {
            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());
            // 根据基金代码查询基金名称
            try {
                FundDetailVO fundDetail = fundInfoService.getFundDetail(entity.getFundCode());
                if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                    vo.setFundName(fundDetail.getBasicInfo().getFundName());
                }
            } catch (Exception e) {
                // 如果查询基金信息失败，不影响交易记录的返回，只是基金名称为空
            }
            return vo;
        }).collect(Collectors.toList());
    
        Map<String, Object> response = new HashMap<>();
        response.put("purchases", transactionVOs);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "查询【当前登录用户】的赎回记录")
    @GetMapping("/my-redemptions")
    public ResponseEntity<Map<String, Object>> getMyRedemptions(Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        List<FundTransaction> transactionEntities = fundTransactionService.listByUserIdAndTransactionType(currentUserId, "赎回");
    
        List<FundTransactionVO> transactionVOs = transactionEntities.stream().map(entity -> {
            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());
            // 根据基金代码查询基金名称
            try {
                FundDetailVO fundDetail = fundInfoService.getFundDetail(entity.getFundCode());
                if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                    vo.setFundName(fundDetail.getBasicInfo().getFundName());
                }
            } catch (Exception e) {
                // 如果查询基金信息失败，不影响交易记录的返回，只是基金名称为空
            }
            return vo;
        }).collect(Collectors.toList());
    
        Map<String, Object> response = new HashMap<>();
        response.put("redemptions", transactionVOs);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "根据交易ID查询【当前用户】的单条交易详情")
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponseVO<FundTransactionVO>> getById(@PathVariable Long id, Authentication authentication) {
        CustomUserDetails userDetails = (CustomUserDetails) authentication.getPrincipal();
        Long currentUserId = userDetails.getId();
        try {
            FundTransaction entity = fundTransactionService.getTransactionByIdAndUserId(id, currentUserId);

            FundTransactionVO vo = new FundTransactionVO();
            BeanUtils.copyProperties(entity, vo);
            // 确保银行卡号字段被正确复制
            vo.setBankAccountNumber(entity.getBankAccountNumber());
            // 根据基金代码查询基金名称
            try {
                FundDetailVO fundDetail = fundInfoService.getFundDetail(entity.getFundCode());
                if (fundDetail != null && fundDetail.getBasicInfo() != null) {
                    vo.setFundName(fundDetail.getBasicInfo().getFundName());
                }
            } catch (Exception e) {
                // 如果查询基金信息失败，不影响交易记录的返回，只是基金名称为空
            }

            return ResponseEntity.ok(ApiResponseVO.success("交易详情获取成功", vo));
        } catch (Exception e) {
            // Service层在找不到或无权访问时会抛出异常
            return ResponseEntity.status(403).body(ApiResponseVO.error(e.getMessage()));
        }
    }
}