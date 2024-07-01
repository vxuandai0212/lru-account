package com.lru.account;

import com.lru.account.lru.AccountLRUCacheThreadSafe;
import com.lru.account.lru.LinkedListNode;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
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
        return lruCache.getLinkedListNodeMap().values()
            .stream()
            .map(LinkedListNode::getElement)
            .sorted(Comparator.comparing(Account::getBalance).reversed())
            .limit(3)
            .toList();
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
