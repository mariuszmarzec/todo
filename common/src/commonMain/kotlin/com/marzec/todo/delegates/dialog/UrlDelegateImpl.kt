package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.mvi.Store3
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.delegate.StoreDelegate
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
    private lateinit var dialogDelegatedStore: DialogDelegate

    override fun init(store: Store3<State<DATA>>) {
        super.init(store)
        urlDelegatedStore = store as UrlDelegate
        dialogDelegatedStore = store as DialogDelegate
    }

    override fun openUrls(urls: List<String>) =
        if (urls.size == 1) {
            openUrl(urls.first())
        } else {
            dialogDelegatedStore.showSelectUrlDialog(urls)
        }

    override fun openUrl(url: String) = sideEffect {
        openUrlHelper.open(url)
    }

    override fun openUrlForTask(task: Task) {
        task.urlToOpen()?.let { openUrl(it) }
    }

    override fun openUrlForTask(id: Int) = sideEffect {
        state.ifDataAvailable {
            urlDelegatedStore.openUrlForTask(taskById(id))
        }
    }
}
