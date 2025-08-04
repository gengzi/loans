package com.gengzi.loan.context;

import com.gengzi.loan.service.ApplyingState;
import com.gengzi.loan.service.LoanState;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class LoanOrder {
    private String orderId; // 订单ID
    private String userId; // 用户ID
    private BigDecimal amount; // 借款金额
    private int term; // 借款期限（月）
    private LoanState currentState; // 当前状态
    private LocalDateTime createTime; // 创建时间
    // 其他属性：剩余本金、逾期天数、还款计划等


    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public int getTerm() {
        return term;
    }

    public void setTerm(int term) {
        this.term = term;
    }

    public LoanState getCurrentState() {
        return currentState;
    }

    public void setCurrentState(LoanState currentState) {
        this.currentState = currentState;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LoanOrder(String orderId, String userId, BigDecimal amount, int term) {
        this.orderId = orderId;
        this.userId = userId;
        this.amount = amount;
        this.term = term;
        this.createTime = LocalDateTime.now();
        this.currentState = new ApplyingState(); // 初始状态为“申请中”
    }

    // 委托当前状态处理行为
    public void review(boolean pass, String reason) {
        currentState.review(this, pass, reason);
    }
//    public void sign() {
//        currentState.sign(this);
//    }
//    public void disburse() {
//        currentState.disburse(this);
//    }
//    public void repay(BigDecimal amount) {
//        currentState.repay(this, amount);
//    }
//    public void overdue() {
//        currentState.overdue(this);
//    }

    // 更新状态（由具体状态类调用）
    public void setState(LoanState state) {
        // 记录状态变更日志（金融场景必须，用于审计）
        logStateChange(state.getStateName());
        this.currentState = state;
    }

    // 记录状态变更日志（含时间、操作人、原因等）
    private void logStateChange(String newState) {
        System.out.printf("[%s] 订单%s 状态变更：%s → %s%n",
                LocalDateTime.now(), orderId, currentState.getStateName(), newState);
    }

    // getter/setter 略
}