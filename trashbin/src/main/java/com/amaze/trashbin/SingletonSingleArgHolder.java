package com.amaze.trashbin;


public class SingletonSingleArgHolder<T, A, B, C> {
    private final TriFunction<A, B, C, T> constructor;

    private volatile T instance;

    public SingletonSingleArgHolder(TriFunction<A, B, C, T> constructor) {
        this.constructor = constructor;
    }

    public T getInstance(A arg1, B arg2, C arg3) {
        if (instance == null) {
            synchronized (this) {
                if (instance == null) {
                    instance = constructor.apply(arg1, arg2, arg3);
                }
            }
        }
        return instance;
    }

    interface TriFunction<A, B, C, R> {
        R apply(A arg1, B arg2, C arg3);
    }
}

