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
}
