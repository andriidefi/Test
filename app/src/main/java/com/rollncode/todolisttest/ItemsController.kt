package com.rollncode.todolisttest

import android.content.Context
import android.util.AttributeSet
import android.util.Log
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout
import com.airbnb.epoxy.EpoxyModelWithView
import com.airbnb.epoxy.TypedEpoxyController
import kotlinx.android.synthetic.main.view_hit_item.view.*
import java.text.DateFormat

/**
 *
 */
class ItemsController(private val checkedItemsListener: (Set<HitItem>) -> Unit) : TypedEpoxyController<List<HitItem>>(),
    View.OnClickListener {
    override fun onClick(v: View?) {
        val item = v?.tag as? HitItem ?: return
        val isChecked = checkedItems.contains(item)
        Log.d("-=-", "onClick $isChecked ${item.title}")
        if (!isChecked) checkedItems.add(item)
        else checkedItems.remove(item)
        checkedItemsListener(checkedItems)
        val checkBox = (v as? HitItemView)?.checkBox ?: return
        checkBox.performClick()
    }

    private val checkedItems = mutableSetOf<HitItem>()

    fun refresh() {
//        checkedItems.clear()
        checkedItemsListener(checkedItems)
        setData(listOf())
    }

    override fun buildModels(data: List<HitItem>?) {
        data ?: return
        add(data.map {
            ItemEpoxyModel(it).id(it.hashCode())
        })
    }

    inner class ItemEpoxyModel(private val item: HitItem) : EpoxyModelWithView<HitItemView>() {
        override fun buildView(parent: ViewGroup): HitItemView =
            HitItemView(parent.context)

        override fun bind(view: HitItemView) {
            super.bind(view)
            view.bind(item)
        }
    }

    inner class HitItemView(context: Context, attributeSet: AttributeSet? = null) : ConstraintLayout(context, attributeSet) {
        init {
            View.inflate(context, R.layout.view_hit_item, this)
        }

        override fun setLayoutParams(params: ViewGroup.LayoutParams?) {
            super.setLayoutParams(params)
            params?.width = ViewGroup.LayoutParams.MATCH_PARENT
            params?.height = ViewGroup.LayoutParams.WRAP_CONTENT
        }

        fun bind(item: HitItem) {
            tag = item
            tvTitle.text = item.title
            tvDate.text = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(item.createdAt)
            checkBox.isChecked = checkedItems.contains(item)
            setOnClickListener(this@ItemsController)
        }
    }

}