package com.acme.mediatranscoding.support

import java.util.concurrent.atomic.AtomicLong

private const val INNER_CLASS_NAME_QUALIFIER = '$'
private const val DOT = '.'

val Class<*>.humanizedName: String get() = name.replace(INNER_CLASS_NAME_QUALIFIER, DOT)

class Counter(private val initial: Long) {

    constructor() : this(0L)

    private val atomic = AtomicLong(initial)

    fun reset() {
        set(initial)
    }

    operator fun plusAssign(amount: Long) {
        while (true) {
            val current = atomic.get()
            val update = current + amount
            if (atomic.compareAndSet(current, update)) {
                break
            }
        }
    }

    fun set(update: Long): Long {
        var old: Long
        do {
            old = atomic.get()
            if (atomic.compareAndSet(old, update)) {
                break
            }
        } while (true)
        return old
    }

    fun get(): Long = atomic.get()

}