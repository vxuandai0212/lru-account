package com.lru.account;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@ToString
@Builder
public class Account {
    private volatile Long id;
    private volatile BigDecimal balance;

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Account other))
            return false;
        boolean balanceEquals = (this.balance == null && other.balance == null)
            || (this.balance != null && this.balance.compareTo(other.balance) != 0);
        return this.id.equals(other.id) && balanceEquals;
    }

}
