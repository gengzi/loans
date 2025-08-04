package com.gengzi.loan.service;


import com.gengzi.loan.context.LoanOrder;

/**
 * 交易状态
 */
public interface LoanState {

    /**
     * 授信审批
     *
     * @param order  订单上下文
     * @param pass   是否通过
     * @param reason 原因
     */
    void review(LoanOrder order, boolean pass, String reason);


    // 获取状态名称
    String getStateName();

}
