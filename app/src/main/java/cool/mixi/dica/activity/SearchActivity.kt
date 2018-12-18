package cool.mixi.dica.activity

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.widget.EditText
import androidx.appcompat.widget.SearchView
import cool.mixi.dica.R
import cool.mixi.dica.bean.Consts

class SearchActivity: BaseActivity() {

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
                searchEditText?.hint = if(that == null || !that){
                    "#friendica"
                } else {
                    searchTerm
                }
            }

            searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                override fun onQueryTextSubmit(query: String?): Boolean {
                    title = query
                    it.collapseActionView()
                    return true
                }

                override fun onQueryTextChange(newText: String?): Boolean {
                    return true
                }

            })
        }

        return super.onCreateOptionsMenu(menu)
    }

    private fun processIntent(){
        searchTerm = intent.getStringExtra(Consts.EXTRA_SEARCH_TERM)
        searchTerm.isNullOrEmpty().let {
            if(it) return
            title = searchTerm
            searchEditText?.setText(searchTerm)
        }
    }
}