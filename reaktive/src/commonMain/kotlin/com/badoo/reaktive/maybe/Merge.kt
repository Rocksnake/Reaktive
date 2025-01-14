package com.badoo.reaktive.maybe

import com.badoo.reaktive.base.ErrorCallback
import com.badoo.reaktive.disposable.CompositeDisposable
import com.badoo.reaktive.disposable.Disposable
import com.badoo.reaktive.disposable.minusAssign
import com.badoo.reaktive.disposable.plusAssign
import com.badoo.reaktive.observable.Observable
import com.badoo.reaktive.observable.observable
import com.badoo.reaktive.observable.serialize
import com.badoo.reaktive.single.Single
import com.badoo.reaktive.utils.ObjectReference
import com.badoo.reaktive.utils.atomic.AtomicInt

/**
 * Merges multiple [Maybe]s into one [Observable], running all [Maybe]s simultaneously.
 *
 * Please refer to the corresponding RxJava [document](http://reactivex.io/RxJava/javadoc/io/reactivex/Maybe.html#merge-java.lang.Iterable-).
 */
fun <T> Iterable<Maybe<T>>.merge(): Observable<T> =
    observable { emitter ->
        val disposables = CompositeDisposable()
        emitter.setDisposable(disposables)
        val serializedEmitter = emitter.serialize()
        val activeSourceCount = AtomicInt(1)

        forEach { upstream ->
            activeSourceCount.addAndGet(1)
            upstream.subscribe(
                object : ObjectReference<Disposable?>(null), MaybeObserver<T>, ErrorCallback by serializedEmitter {
                    override fun onSubscribe(disposable: Disposable) {
                        value = disposable
                        disposables += disposable
                    }

                    override fun onSuccess(value: T) {
                        serializedEmitter.onNext(value)
                        onComplete()
                    }

                    override fun onComplete() {
                        disposables -= requireNotNull(value)
                        if (activeSourceCount.addAndGet(-1) == 0) {
                            emitter.onComplete()
                        }
                    }
                }
            )
        }

        if (activeSourceCount.addAndGet(-1) == 0) {
            emitter.onComplete()
        }
    }

/**
 * Merges multiple [Maybe]s into one [Observable], running all [Single]s simultaneously.
 *
 * Please refer to the corresponding RxJava [document](http://reactivex.io/RxJava/javadoc/io/reactivex/Maybe.html#merge-java.lang.Iterable-).
 */
fun <T> merge(vararg sources: Maybe<T>): Observable<T> =
    sources
        .asList()
        .merge()
