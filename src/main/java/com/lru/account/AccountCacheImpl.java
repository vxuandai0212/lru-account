package com.lru.account;

import com.lru.account.lru.AccountLRUCacheThreadSafe;
import lombok.Getter;

import java.util.*;
import java.util.function.Consumer;

@Getter // debug to investigate the cache
public class AccountCacheImpl implements AccountCache {

    private final AccountLRUCacheThreadSafe lruCache;

    public AccountCacheImpl(int size) {
        this.lruCache = new AccountLRUCacheThreadSafe(size);
    }

    @Override
    public Account getAccountById(long id) {
        return lruCache.get(id).orElse(null);
    }

    @Override
    public void subscribeForAccountUpdates(Consumer<Account> listener) {
        lruCache.delegateListener(listener);
    }

    @Override
    public List<Account> getTop3AccountsByBalance() {
        int max = 3;
        int init = 0;
        Iterator<Map.Entry<Account, Long>> iterator = lruCache.getTops().entrySet().iterator();
        List<Account> result = new ArrayList<>();
        while (iterator.hasNext() && init < max) {
            result.add(iterator.next().getKey());
            init++;
        }
        return result;
    }

    @Override
    public int getAccountByIdHitCount() {
        return lruCache.getHits();
    }

    @Override
    public void putAccount(Account account) {
        lruCache.put(account.getId(), account);
    }
}
