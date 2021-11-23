package com.marzec.todo.delegates.dialog

import com.marzec.mvi.State
import com.marzec.todo.common.OpenUrlHelper
import com.marzec.todo.delegates.StoreDelegate

interface UrlDelegate<DATA : WithDialog<DATA>> {
    fun openUrls(urls: List<String>)
    fun openUrl(url: String)
    fun showSelectUrlDialog(urls: List<String>)
}

class UrlDelegateImpl<DATA : WithDialog<DATA>>(
    private val openUrlHelper: OpenUrlHelper,
    private val dialogDelegate: DialogDelegate
) : StoreDelegate<State<DATA>>(), UrlDelegate<DATA> {

    override fun openUrls(urls: List<String>) =
        if (urls.size == 1) {
            openUrl(urls.first())
        } else {
            showSelectUrlDialog(urls)
        }

    override fun openUrl(url: String) = sideEffectIntent {
        openUrlHelper.open(url)
    }

    override fun showSelectUrlDialog(urls: List<String>) =
        dialogDelegate.showSelectUrlDialog(urls)

}
