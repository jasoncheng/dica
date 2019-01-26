package cool.mixi.dica.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Status
import cool.mixi.dica.database.AppDatabase
import cool.mixi.dica.util.ApiService
import cool.mixi.dica.util.IStatusDataSource
import cool.mixi.dica.util.StatusTimeline
import cool.mixi.dica.util.toHashTag
import kotlinx.android.synthetic.main.activity_search.*
import retrofit2.Call

class SearchActivity: BaseActivity(), IStatusDataSource {

    private var searchView:SearchView? = null
    private var searchEditText:EditText?= null
    private var searchTerm:String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search)
        processIntent()
    }

    override fun onBackPressed() {
        searchView?.let {
            if(!it.isIconified){
                it.onActionViewCollapsed()
                return
            }
        }
        super.onBackPressed()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        processIntent()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.search, menu)
        menu?.findItem(R.id.menu_search)?.let {
            searchView = it.actionView as SearchView
            searchEditText = searchView!!.findViewById(androidx.appcompat.R.id.search_src_text)
            searchTerm?.isNullOrEmpty().let { that ->
                searchEditText?.hint = if (that == null || !that) {
                    "#${getString(R.string.app_name)}"
                } else {
                    searchTerm
                }
            }

            searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    query?.let { that ->
                        var prefix = ""
                        if(!Consts.ENABLE_FULL_TEXT_SEARCH && !that.startsWith("#", true)){
                            prefix = "#"
                        }

                        title = "$prefix$that".trim()
                        searchTerm = title.toString()
                        it.collapseActionView()
                        resetSearch()

                    }
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {

                    return true
                }

            })
            searchView?.setOnSearchClickListener {
                searchEditText!!.setText(searchTerm)
                searchEditText!!.setSelection(searchTerm!!.length)
            }
            searchView?.setOnCloseListener { false }
        }
        return super.onCreateOptionsMenu(menu)
    }

    override fun requireRefresh() {
        resetSearch()
    }

    private fun resetSearch(){
        stl?.let {
            it.maxId = 0
            it.sinceId = 0
        }
        home_srl.isRefreshing = true
        stl?.clear()
        rv_statuses_list.adapter?.notifyDataSetChanged()
        stl?.loadNewest(null)
        AppDatabase.upsertHashTag(searchTerm!!)
    }

    private fun processIntent(){
        searchTerm = intent.getStringExtra(Consts.EXTRA_SEARCH_TERM).toHashTag()
        searchTerm.isNullOrEmpty().let {
            if(it) return
            title = searchTerm
            searchEditText?.setText(searchTerm)
            AppDatabase.upsertHashTag(searchTerm!!)
            initLoad()
        }
    }

    private fun initLoad(){
        home_srl.isRefreshing = true
        stl = StatusTimeline(this, rv_statuses_list, home_srl, this).init()
        stl?.loadNewest(null)
    }

    override fun loaded(data: List<Status>) {}

    override fun sourceOld(): Call<List<Status>>? {
        return ApiService.create().search("$searchTerm", "","${stl?.maxId}")
    }

    override fun sourceNew(): Call<List<Status>>? {
        return ApiService.create().search("$searchTerm","${stl?.sinceId}", "")
    }
}