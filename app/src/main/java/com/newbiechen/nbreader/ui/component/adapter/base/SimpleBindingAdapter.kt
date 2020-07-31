package com.newbiechen.nbreader.ui.component.adapter.base

import android.view.ViewGroup
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.RecyclerView

typealias OnItemClickListener<T> = (pos: Int, value: T) -> Unit

/**
 * 简单封装基于 DataBinding 的 Adapter
 */
abstract class SimpleBindingAdapter<T> : RecyclerView.Adapter<WrapViewHolder<T>>() {
    private val mItemList: ArrayList<T> = ArrayList()

    abstract fun createViewHolder(type: Int): IViewHolder<T>

    open fun bindViewHolder(binding: ViewDataBinding, position: Int) {
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WrapViewHolder<T> {
        val holder = createViewHolder(viewType)
        // 创建 binding
        val binding = holder.createBinding(parent)
        return WrapViewHolder(binding, holder)
    }

    override fun getItemCount(): Int = mItemList.size

    final override fun onBindViewHolder(holder: WrapViewHolder<T>, position: Int) {
        holder.holder.onBind(getItem(position)!!, position)
        bindViewHolder(holder.binding, position)
    }

    fun getItem(pos: Int): T? {
        return mItemList.getOrNull(pos)
    }

    fun refreshItems(items: List<T>?) {
        if (items == null || items.isEmpty()) {
            return
        }

        mItemList.clear()
        mItemList.addAll(items)
        notifyDataSetChanged()
    }
}

