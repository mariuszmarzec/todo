package com.marzec.todo.delegates.dialog

import com.marzec.common.OpenUrlHelper
import com.marzec.delegate.DialogDelegate
import com.marzec.mvi.StoreDelegate
import com.marzec.mvi.State
import com.marzec.mvi.Store4
import com.marzec.todo.extensions.urlToOpen
import com.marzec.todo.model.Task

interface UrlDelegate {
    fun openUrls(urls: List<String>)
    fun openUrl(url: String)
    fun openUrlForTask(task: Task)
    fun openUrlForTask(id: Int)
}

class UrlDelegateImpl<DATA : WithTasks<DATA>>(
    private val openUrlHelper: OpenUrlHelper
) : StoreDelegate<State<DATA>>(), UrlDelegate {

    private lateinit var urlDelegatedStore: UrlDelegate
    private lateinit var dialogDelegatedStore: DialogDelegate<Int>

    override fun init(store: Store4<State<DATA>>) {
        super.init(store)
        urlDelegatedStore = store as UrlDelegate
        dialogDelegatedStore = store as DialogDelegate<Int>
    }

    override fun openUrls(urls: List<String>) =
        if (urls.size == 1) {
            openUrl(urls.first())
        } else {
            dialogDelegatedStore.showSelectUrlDialog(urls)
        }

    override fun openUrl(url: String) = sideEffectIntent {
        openUrlHelper.open(url)
    }

    override fun openUrlForTask(task: Task) {
        task.urlToOpen()?.let { openUrl(it) }
    }

    override fun openUrlForTask(id: Int) = sideEffectIntent {
        state.ifDataAvailable {
            urlDelegatedStore.openUrlForTask(taskById(id))
        }
    }
}
