package com.rollncode.todolisttest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

@Suppress("UNCHECKED_CAST")
class MainActivity : AppCompatActivity() {

    private val factory by lazy {
        object : ViewModelProvider.Factory {
            override fun <T : ViewModel?> create(modelClass: Class<T>): T {
                return MainViewModel(this@MainActivity) as T
            }
        }
    }
    private val viewModel: MainViewModel by lazy { ViewModelProviders.of(this, factory)[MainViewModel::class.java] }
    private val controller = ItemsController {
        supportActionBar?.title = getString(R.string.selected_items_format, it.size)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolBar)
        supportActionBar?.title = getString(R.string.selected_items_format, 0)
        viewModel.itemsData.observe(this, Observer {
            controller.setData(it)
        })
        viewModel.isLoading.observe(this, Observer {
            swipeRefreshLayout.isRefreshing = it == true
        })
        recyclerView.adapter = controller.adapter
        swipeRefreshLayout.setOnRefreshListener {
            viewModel.getData(true)
            controller.refresh()
        }
        recyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisible = layoutManager.findLastVisibleItemPosition()

                val endHasBeenReached = lastVisible + 5 >= totalItemCount
                if (totalItemCount > 0 && endHasBeenReached) {
                    viewModel.getData()
                }
            }
        })
        viewModel.getData()
    }
}
