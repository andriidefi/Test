package com.rollncode.todolisttest

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MainViewModel
    private val controller = ItemsController {
        supportActionBar?.title = getString(R.string.selected_items_format, it.size)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolBar)
        supportActionBar?.title = getString(R.string.selected_items_format, 0)
        viewModel = MainViewModel(this)
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
