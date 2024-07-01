package com.lru.account.lru;

import com.lru.account.Account;
import lombok.Getter;

import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Consumer;

@Getter // debug to investigate the cache
public class AccountLRUCacheThreadSafe implements Cache<Long, Account> {
    private final int size;
    private final Map<Long, LinkedListNode<Account>> linkedListNodeMap;
    private final DoublyLinkedList<Account> doublyLinkedList;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
    private final AtomicInteger hits = new AtomicInteger(0);
    private final ConcurrentSkipListMap<Account, Long> tops;
    private Consumer<Account> listener;

    public AccountLRUCacheThreadSafe(int size) {
        this.size = size;
        this.linkedListNodeMap = new ConcurrentHashMap<>(size);
        this.doublyLinkedList = new DoublyLinkedList<>();
        this.tops = new ConcurrentSkipListMap<>(Comparator.comparing(Account::getBalance));
    }

    @Override
    public boolean put(Long key, Account value) {
        this.lock.writeLock().lock();
        try {
            LinkedListNode<Account> newNode;
            if (this.linkedListNodeMap.containsKey(key)) {
                LinkedListNode<Account> node = this.linkedListNodeMap.get(key);
                if (node.getElement().getBalance().compareTo(value.getBalance()) != 0) {
                    listener.accept(value);
                    this.tops.remove(node.getElement());
                    this.tops.put(value, value.getId());
                }
                newNode = doublyLinkedList.updateAndMoveToFront(node, value);
            } else {
                if (this.size() >= this.size) {
                    this.evictElement();
                }
                listener.accept(value);
                this.tops.put(value, value.getId());
                newNode = this.doublyLinkedList.add(value);
            }
            if (newNode.isEmpty()) {
                return false;
            }
            this.linkedListNodeMap.put(key, newNode);
            return true;
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<Account> get(Long key) {
        this.lock.readLock().lock();
        try {
            LinkedListNode<Account> linkedListNode = this.linkedListNodeMap.get(key);
            if (linkedListNode != null && !linkedListNode.isEmpty()) {
                hits.incrementAndGet();
                linkedListNodeMap.put(key, this.doublyLinkedList.moveToFront(linkedListNode));
                return Optional.of(linkedListNode.getElement());
            }
            return Optional.empty();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        this.lock.readLock().lock();
        try {
            return doublyLinkedList.size();
        } finally {
            this.lock.readLock().unlock();
        }
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public void clear() {
        this.lock.writeLock().lock();
        try {
            linkedListNodeMap.clear();
            tops.clear();
            doublyLinkedList.clear();
        } finally {
            this.lock.writeLock().unlock();
        }
    }

    @Override
    public int getHits() {
        return this.hits.get();
    }

    @Override
    public void delegateListener(Consumer<Account> listener) {
        this.listener = listener;
    }

    private void evictElement() {
        this.lock.writeLock().lock();
        try {
            LinkedListNode<Account> linkedListNode = doublyLinkedList.removeTail();
            if (linkedListNode.isEmpty()) {
                return;
            }
            linkedListNodeMap.remove(linkedListNode.getElement().getId());
            tops.remove(linkedListNode.getElement());
        } finally {
            this.lock.writeLock().unlock();
        }
    }
}
