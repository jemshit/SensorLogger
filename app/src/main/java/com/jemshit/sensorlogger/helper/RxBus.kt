package com.jemshit.sensorlogger.helper

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.subjects.PublishSubject
import javax.annotation.Nonnull

object RxBus {
    private val publisher = PublishSubject.create<Any>().toSerialized()

    fun publish(@Nonnull event: Any) = publisher.onNext(event)

    fun <T> listen(eventClass: Class<T>): Observable<T> {
        return publisher
                .ofType(eventClass)
    }

    fun <T> listen(eventClass: Class<T>, observeScheduler: Scheduler): Observable<T> {
        return publisher
                .ofType(eventClass)
                .observeOn(observeScheduler)
    }
}