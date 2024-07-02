package com.lru.account;

import com.lru.account.lru.AccountLRUCacheThreadSafe;
import com.lru.account.lru.Cache;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Slf4j
class AccountApplicationTests {

    @Test
    void runMultiThreadTask_WhenPutDataInConcurrentToCache_ThenNoDataLost() throws Exception {
        final int size = 500000;
        Cache<Long, Account> cache;
        try (ExecutorService executorService = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors())) {
            cache = new AccountLRUCacheThreadSafe(size);
            cache.delegateListener(account -> log.info("Listener: {}", account.toString()));
            CountDownLatch countDownLatch = new CountDownLatch(size);
            IntStream.range(0, size).<Runnable>mapToObj(key -> () -> {
                cache.put((long) key, Account.builder().id((long) key).balance(BigDecimal.valueOf(key)).build());
                countDownLatch.countDown();
            }).forEach(executorService::submit);
            countDownLatch.await();
        }
        assertEquals(cache.size(), size);
        IntStream.range(0, size).forEach(i -> assertEquals(BigDecimal.valueOf(i), cache.get((long) i).get().getBalance()));
    }

    @Test
    void testAccountCacheImplMethods() {
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

        Account E1 = Account.builder()
            .id(5L)
            .balance(BigDecimal.valueOf(515))
            .build();

        Account E2 = Account.builder()
            .id(6L)
            .balance(BigDecimal.valueOf(556))
            .build();

        Account E3 = Account.builder()
            .id(7L)
            .balance(BigDecimal.valueOf(570))
            .build();

        Account F = Account.builder()
            .id(8L)
            .balance(BigDecimal.valueOf(60))
            .build();

        AccountCache service = new AccountCacheImpl(5);
        service.subscribeForAccountUpdates(consumer);
        service.putAccount(A);
        service.putAccount(B);
        service.putAccount(E1);
        service.putAccount(E2);
        service.putAccount(E3);
        assertEquals(0, service.getAccountByIdHitCount());
        service.getLruCache().getLinkedListNodeMap().values().forEach(e-> log.info(e.getElement().toString()));
        assertTrue(service.getTop3AccountsByBalance().stream().map(Account::getId).toList().containsAll(List.of(5L, 6L, 7L)));
        service.putAccount(C);
        service.getAccountById(B.getId());
        service.putAccount(D);
        service.getLruCache().getLinkedListNodeMap().values().forEach(e-> log.info(e.getElement().toString()));
        assertTrue(service.getTop3AccountsByBalance().stream().map(Account::getId).toList().containsAll(List.of(4L, 6L, 7L)));
        service.putAccount(E);
        service.putAccount(F);
        service.getLruCache().getLinkedListNodeMap().values().forEach(e-> log.info(e.getElement().toString()));
        assertTrue(service.getTop3AccountsByBalance().stream().map(Account::getId).toList().containsAll(List.of(8L, 3L, 8L)));
        assertEquals(1, service.getAccountByIdHitCount());
    }

    @Test
    void checkConcurrentSkipListMap() {
        ConcurrentSkipListMap<Account, Long> tops = new ConcurrentSkipListMap<>(Comparator.comparing(Account::getBalance));
        Map<Long, Account> linkedListNodeMap = new ConcurrentHashMap<>();
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
            .id(3L)
            .balance(BigDecimal.valueOf(40))
            .build();

        log.info("tops");
        tops.put(A, A.getId());
        tops.put(B, A.getId());
        tops.put(C, A.getId());
        tops.put(D, D.getId());
        tops.keySet().forEach(e -> log.info(e.toString()));

        log.info("linkedListNodeMap");
        linkedListNodeMap.put(A.getId(), A);
        linkedListNodeMap.put(B.getId(), B);
        linkedListNodeMap.put(C.getId(), C);
        linkedListNodeMap.put(D.getId(), D);
        linkedListNodeMap.values().forEach(e -> log.info(e.toString()));
    }

}
