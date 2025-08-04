package com.gengzi.loan.service;

import com.gengzi.loan.context.LoanOrder;

public class ApplyingState implements LoanState{
    @Override
    public void review(LoanOrder order, boolean pass, String reason) {

    }

    @Override
    public String getStateName() {
        return "";
    }
}
