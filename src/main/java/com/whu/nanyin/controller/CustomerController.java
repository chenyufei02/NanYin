package com.whu.nanyin.controller;

import com.whu.nanyin.pojo.dto.CustomerDTO;
import com.whu.nanyin.pojo.dto.CustomerUpdateDTO;
import com.whu.nanyin.pojo.entity.Customer;
//import com.whu.nanyin.pojo.vo.CustomerVO;
//import java.util.stream.Collectors;
import com.whu.nanyin.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;


/**
 * 客户管理API控制器
 * 提供所有与客户相关的、返回JSON数据格式的接口。
 * 注意：所有返回页面的逻辑在 PageController
 */
@RestController // 使用 @RestController，表明此类所有方法都返回数据，而非视图。
@RequestMapping("/api/customer") // 为所有API路径统一添加 /api 前缀，方便管理和部署。
@Tag(name = "客户管理", description = "客户相关增删改查的数据接口")
public class CustomerController {

    @Autowired
    private CustomerService customerService;


    /**
     * 【改】处理更新客户信息的API。
     * @param dto 包含客户更新信息的DTO对象
     * @return 操作是否成功
     */
    @Operation(summary = "更新客户信息")
    @PutMapping("/update")
    public boolean updateCustomer(@RequestBody @Validated CustomerUpdateDTO dto) {
        Customer customer = new Customer();
        BeanUtils.copyProperties(dto, customer);
        return customerService.updateCustomer(customer);
    }





}