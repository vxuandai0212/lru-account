package com.lru.account.lru;

import com.lru.account.Account;

import java.util.Optional;
import java.util.function.Consumer;

public interface Cache<K, V> {

    boolean put(K key, V value);

    Optional<V> get(K key);

    int size();

    boolean isEmpty();

    void clear();

    int getHits();

    void delegateListener(Consumer<Account> listener);

}
