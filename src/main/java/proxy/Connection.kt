package proxy

import io.reactivex.BackpressureStrategy.LATEST
import io.reactivex.Flowable
import io.reactivex.Scheduler
import io.reactivex.subjects.BehaviorSubject
import proxy.adapter.model.Message

class Connection(
    private val scheduler: Scheduler
) {
    private val subject = BehaviorSubject.create<Message>()

    fun send(message: Message) {
        subject.onNext(message)
    }

    fun observeEvents(): Flowable<Message> {
        return subject.toFlowable(LATEST).share().hide()
    }
}