package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.delegates.StoreDelegate

class UrlDelegate<DATA : WithDialog<DATA>>(
    private val openUrlHelper: OpenUrlHelper,
    private val dialogDelegate: DialogDelegate<DATA>
) : StoreDelegate<State<DATA>>() {

    fun openUrls(urls: List<String>) =
        if (urls.size == 1) {
            openUrl(urls.first())
        } else {
            showSelectUrlDialog(urls)
        }

    fun openUrl(url: String) = sideEffectIntent {
        openUrlHelper.open(url)
    }

    private fun showSelectUrlDialog(urls: List<String>) =
        dialogDelegate.showSelectUrlDialog(urls)
}
