package com.lru.account;

import com.lru.account.lru.AccountLRUCacheThreadSafe;
import com.lru.account.lru.Cache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class AccountApplicationTests {

    @Test
    public void runMultiThreadTask_WhenPutDataInConcurrentToCache_ThenNoDataLost() throws Exception {
        final int size = 50;
        Cache<Long, Account> cache;
        try (ExecutorService executorService = Executors.newFixedThreadPool(5)) {
            cache = new AccountLRUCacheThreadSafe(size);
            CountDownLatch countDownLatch = new CountDownLatch(size);
            try {
                IntStream.range(0, size).<Runnable>mapToObj(key -> () -> {
                    cache.put((long) key, Account.builder().id((long) key).balance(BigDecimal.valueOf(key)).build());
                    countDownLatch.countDown();
                }).forEach(executorService::submit);
                countDownLatch.await();
            } finally {
                executorService.shutdown();
            }
        }
        assertEquals(cache.size(), size);
        IntStream.range(0, size).forEach(i -> assertEquals(BigDecimal.valueOf(i), cache.get((long) i).get().getBalance()));
    }

    @Test
    public void testAccountCacheImplMethods() {
        Consumer<Account> consumer = account -> log.info("From listener: {}", account.toString());

        Account A = Account.builder()
                .id(1L)
                .balance(BigDecimal.valueOf(10))
                .build();

        Account B = Account.builder()
                .id(2L)
                .balance(BigDecimal.valueOf(20))
                .build();

        Account C = Account.builder()
                .id(3L)
                .balance(BigDecimal.valueOf(30))
                .build();

        Account D = Account.builder()
                .id(4L)
                .balance(BigDecimal.valueOf(40))
                .build();

        Account E = Account.builder()
                .id(5L)
                .balance(BigDecimal.valueOf(5))
                .build();

        AccountCache service = new AccountCacheImpl(2);
        service.subscribeForAccountUpdates(consumer);
        service.putAccount(A);
        service.putAccount(B);
        assertEquals(0, service.getAccountByIdHitCount());
        assertTrue(service.getTop3AccountsByBalance().stream().map(Account::getId).toList().containsAll(List.of(1L, 2L)));
        service.putAccount(C);
        service.getAccountById(B.getId());
        service.putAccount(D);
        assertTrue(service.getTop3AccountsByBalance().stream().map(Account::getId).toList().containsAll(List.of(4L, 2L)));
        service.putAccount(E);
        assertTrue(service.getTop3AccountsByBalance().stream().map(Account::getId).toList().containsAll(List.of(4L, 5L)));
        assertEquals(1, service.getAccountByIdHitCount());
    }

}
