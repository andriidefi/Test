package com.rollncode.todolisttest

import android.app.Activity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.MutableLiveData
import com.google.gson.annotations.SerializedName
import io.reactivex.SingleTransformer
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.internal.schedulers.IoScheduler
import java.util.*

/**
 *
 */
class MainViewModel(activity: Activity) : AndroidViewModel(activity.application) {

    private var page = 1
    private var isMoreAvailable = true
    private val itemsList = mutableListOf<HitItem>()
    private val compositeDisposable = CompositeDisposable()
    val itemsData = MutableLiveData<List<HitItem>>()
    val isLoading = MutableLiveData<Boolean>()

    fun getData(refresh: Boolean = false) {
        if (refresh) {
            itemsList.clear()
            page = 1
        }
        if (!isMoreAvailable || isLoading.value == true) return
        compositeDisposable.add(
            ApiClient.instance.getItems(page)
                .compose(createRequestTransformer())
                .subscribe ({
                    onGetData(it.hits)
                    page = it.page + 1
                }, {

                })
        )
    }

    private fun onGetData(list: List<HitItem>?) {
        if (list.isNullOrEmpty()) {
            isMoreAvailable = false
            return
        }
        itemsList.addAll(list)
        itemsData.postValue(itemsList)
    }

    private fun <T> createRequestTransformer() = SingleTransformer<T, T> { upstream ->
        upstream
            .observeOn(IoScheduler())
            .doOnSubscribe { isLoading.postValue(true) }
            .doOnError { isLoading.postValue(false) }
            .doOnSuccess { isLoading.postValue(false) }
    }

    override fun onCleared() {
        super.onCleared()
        compositeDisposable.clear()
    }
}

data class Response(
    val hits: List<HitItem>?,
val page: Int
)

data class HitItem(
@SerializedName("created_at") val createdAt: Date,
@SerializedName("title") val title: String

)