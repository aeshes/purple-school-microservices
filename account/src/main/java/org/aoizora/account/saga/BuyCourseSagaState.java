package org.aoizora.account.saga;

import org.aoizora.account.dao.domain.User;

public abstract class BuyCourseSagaState {

    private BuyCourseSaga saga;

    public void setContext(BuyCourseSaga saga) {
        this.saga = saga;
    }

    public abstract User pay();
    public abstract User checkPayment();
    public abstract User cancel();
}
