#### guava cache的使用

cache方案：创建一个LoadingCache，并实现if cached, return; otherwise create/load/compute, cache and return;也就是说，我们在改造这个Cache的时候，创建一个CacheLoader，让其进行自动计算，自动加载缓存功能。

##### cache特点

1. 数据是以key-value的方式保存在内存中。
2. 处理过期，动态加载等特点。
3. 过期处理采用算法是LRU，最近最少使用算法。淘汰“最后访问时间”较早的。
4. 只需要声明一个static的对象，就可以开辟空间作为本地缓存。

##### 回收策略

1. 基于引用的回收
   * CacheBuilder.weakKeys()可以定义key为弱引用类型。当键没有其他强引用或者软引用的时候，缓存项可以被垃圾回收。因为垃圾回收仅依赖恒等式（==），使用弱引用键的缓存用==而不是equals比较键。
   * CacheBuilder.weakValues()使用弱引用存储值。当值没有其它（强或软）引用时，缓存项可以被垃圾回收。因为垃圾回收仅依赖恒等式（==），使用弱引用值的缓存用==而不是equals比较值。
   * CacheBuilder.softValues()定义value值为软引用。软引用只有在响应内存需要时，才按照全局最近最少使用的顺序回收。使用软引用值的缓存同样用==而不是equals比较值。
2. 基于时间的回收
   * **expireAfterAccess(long duration, TimeUnit unit)**，表示在单位时间内缓存项没有被访问了，缓存过期，可以被回收。
   * **expireAfterWrite(long duration, TimeUnit unit)**，表示在单位时间内缓存项没有被写，缓存过期，可以被回收，如果需要加载数据（load()），那么为了防止缓存失效瞬间高并发下产生的数据库雪崩问题，会在数据回源的时候，只让一个线程去数据库加载数据，然后其他线程等待，直到线程加载完数据之后，放入缓存，其他线程从缓存中读取数据。缺点就是:其他线程被阻塞了！
   * **refreshAfterWrite(long duration, TimeUnit unit)**， 表示单位时间内缓存项没有被写入，则重新加载数据到缓存中。但是这里是只让一个线程去加载数据，其他数据则返回原来的值，样有效地可以减少等待和锁竞争导致的阻塞，所以refreshAfterWrite会比expireAfterWrite性能好。但是它也有一个缺点，因为到达指定时间后，它不能严格保证所有的查询都获取到新值。guava cache并没使用额外的线程去做定时清理和加载的功能，而是依赖于查询请求。在查询的时候去比对上次更新的时间，如超过设置的时间会选取1个线程进行加载或刷新。所以，如果使用refreshAfterWrite，在吞吐量很低的情况下，如很长一段时间内没有查询之后产生瞬时并发的场景下，由于请求不等待缓存的加载完成而是直接返回缓存中的旧值，这个旧值有可能是很长时间之前的数据，这将会在一些时效性很高的场景下引发问题。
3. 基于容量的回收

##### 回收分析

~~~java
 @Nullable
    V get(Object key, int hash) {
      try {
        if (count != 0) { // read-volatile
          long now = map.ticker.read();
          ReferenceEntry<K, V> e = getLiveEntry(key, hash, now);
          if (e == null) {
            return null;
          }

          V value = e.getValueReference().get();
          if (value != null) {
            recordRead(e, now);
            return scheduleRefresh(e, e.getKey(), hash, value, now, map.defaultLoader);
          }
          tryDrainReferenceQueues();
        }
        return null;
      } finally {
        postReadCleanup();
      }
    }
~~~

 getLiveEntry(key, hash, now);获取当前还存活的Entry。

~~~java
 @Nullable
    ReferenceEntry<K, V> getLiveEntry(Object key, int hash, long now) {
      ReferenceEntry<K, V> e = getEntry(key, hash);
      if (e == null) {
        return null;
      } else if (map.isExpired(e, now)) {
        tryExpireEntries(now);
        return null;
      }
      return e;
    }
~~~

map.isExpired(e, now)获取到的缓存是否过期。

~~~java
 boolean isExpired(ReferenceEntry<K, V> entry, long now) {
    checkNotNull(entry);
    if (expiresAfterAccess()
        && (now - entry.getAccessTime() >= expireAfterAccessNanos)) {
      return true;
    }
    if (expiresAfterWrite()
        && (now - entry.getWriteTime() >= expireAfterWriteNanos)) {
      return true;
    }
    return false;
  }
~~~

当前时间和写入最后时间进行比较，判断是否过期，如果过期，继续进一步处理：tryExpireEntries(now);

~~~java
 void tryExpireEntries(long now) {
      if (tryLock()) {
        try {
          expireEntries(now);
        } finally {
          unlock();
          // don't call postWriteCleanup as we're in a read
        }
      }
    }
~~~

同步方式去清理过期缓存。

~~~java
@GuardedBy("this")
    void expireEntries(long now) {
      drainRecencyQueue();

      ReferenceEntry<K, V> e;
      while ((e = writeQueue.peek()) != null && map.isExpired(e, now)) {
        if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
          throw new AssertionError();
        }
      }
      while ((e = accessQueue.peek()) != null && map.isExpired(e, now)) {
        if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
          throw new AssertionError();
        }
      }
    }
~~~

drainRecencyQueue();

~~~java
@GuardedBy("this")
    void drainRecencyQueue() {
      ReferenceEntry<K, V> e;
      while ((e = recencyQueue.poll()) != null) {
        // An entry may be in the recency queue despite it being removed from
        // the map . This can occur when the entry was concurrently read while a
        // writer is removing it from the segment or after a clear has removed
        // all of the segment's entries.
        if (accessQueue.contains(e)) {
          accessQueue.add(e);
        }
      }
    }
~~~

到此为止过期处理已经完毕了。我们接下来继续看：

~~~java
 @Nullable
    V get(Object key, int hash) {
      try {
        if (count != 0) { // read-volatile
          long now = map.ticker.read();
          ReferenceEntry<K, V> e = getLiveEntry(key, hash, now);
          if (e == null) {
            return null;
          }

          V value = e.getValueReference().get();
          if (value != null) {
            recordRead(e, now);
            return scheduleRefresh(e, e.getKey(), hash, value, now, map.defaultLoader);
          }
          tryDrainReferenceQueues();
        }
        return null;
      } finally {
        postReadCleanup();
      }
    }
~~~

如果获取到了缓存项不为null，则需要记录一些命中率的信息。 recordRead(e, now);

然后定时刷新scheduleRefresh(e, e.getKey(), hash, value, now, map.defaultLoader);由此可见，我们是先进行过期处理，然后在进行刷新处理的。

~~~java
V scheduleRefresh(ReferenceEntry<K, V> entry, K key, int hash, V oldValue, long now,
        CacheLoader<? super K, V> loader) {
      if (map.refreshes() && (now - entry.getWriteTime() > map.refreshNanos)
          && !entry.getValueReference().isLoading()) {
        V newValue = refresh(key, hash, loader, true);
        if (newValue != null) {
          return newValue;
        }
      }
      return oldValue;
    }
~~~

这里判断是否需要刷新缓存（构造缓存的时候指定），判断是否过期，判断当前非loading状态，则进行刷新数据操作。

entry.getValueReference().isLoading()主要判断是不是有

~~~java
 @Nullable
    V refresh(K key, int hash, CacheLoader<? super K, V> loader, boolean checkTime) {
      final LoadingValueReference<K, V> loadingValueReference =
          insertLoadingValueReference(key, hash, checkTime);
      if (loadingValueReference == null) {
        return null;
      }

      ListenableFuture<V> result = loadAsync(key, hash, loadingValueReference, loader);
      if (result.isDone()) {
        try {
          return Uninterruptibles.getUninterruptibly(result);
        } catch (Throwable t) {
          // don't let refresh exceptions propagate; error was already logged
        }
      }
      return null;
    }
~~~

来看看我们去loading数据的方法：insertLoadingValueReference()

~~~java
 @Nullable
    LoadingValueReference<K, V> insertLoadingValueReference(final K key, final int hash,
        boolean checkTime) {
      ReferenceEntry<K, V> e = null;
      lock();
      try {
        long now = map.ticker.read();
        preWriteCleanup(now);

        AtomicReferenceArray<ReferenceEntry<K, V>> table = this.table;
        int index = hash & (table.length() - 1);
        ReferenceEntry<K, V> first = table.get(index);

        // Look for an existing entry.
        for (e = first; e != null; e = e.getNext()) {
          K entryKey = e.getKey();
          if (e.getHash() == hash && entryKey != null
              && map.keyEquivalence.equivalent(key, entryKey)) {
            // We found an existing entry.

            ValueReference<K, V> valueReference = e.getValueReference();
            if (valueReference.isLoading()
                || (checkTime && (now - e.getWriteTime() < map.refreshNanos))) {
              // refresh is a no-op if loading is pending
              // if checkTime, we want to check *after* acquiring the lock if refresh still needs
              // to be scheduled
              return null;
            }

            // continue returning old value while loading
            ++modCount;
            LoadingValueReference<K, V> loadingValueReference =
                new LoadingValueReference<K, V>(valueReference);
            e.setValueReference(loadingValueReference);
            return loadingValueReference;
          }
        }

        ++modCount;
        LoadingValueReference<K, V> loadingValueReference = new LoadingValueReference<K, V>();
        e = newEntry(key, hash, first);
        e.setValueReference(loadingValueReference);
        table.set(index, e);
        return loadingValueReference;
      } finally {
        unlock();
        postWriteCleanup();
      }
    }
~~~

我们可以看到这个方法为了防止高并发，所以进入这个方法的时候，加锁了 lock();同一时刻，只有一个线程执行。

##### LRU回收策略

在缓存实现类中维护了两个双链表Queue，分别为：AccessQueue以及WriteQueue，当添加Entry的时候，将Entry添加到AccessQueue以及WriteQueue末尾。当读取Entry的时候，将Entry添加到AccessQueue的末尾。每次更新一个Entry，则将Entry添加到WriteQueue的末尾。所以每次调用缓存方法之前，都看Entry过期没有，一旦过期，就将该Entry从这两个Queue上和Cache中移除。清理代码如下：

~~~java
 @GuardedBy("this")
    void expireEntries(long now) {
      drainRecencyQueue();

      ReferenceEntry<K, V> e;
      while ((e = writeQueue.peek()) != null && map.isExpired(e, now)) {
        if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
          throw new AssertionError();
        }
      }
      while ((e = accessQueue.peek()) != null && map.isExpired(e, now)) {
        if (!removeEntry(e, e.getHash(), RemovalCause.EXPIRED)) {
          throw new AssertionError();
        }
      }
    }
~~~

