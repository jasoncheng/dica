package cool.mixi.dica.database.dao

import android.arch.persistence.room.Dao
import android.arch.persistence.room.Insert
import android.arch.persistence.room.Query
import cool.mixi.dica.bean.Meta
import cool.mixi.dica.database.AppDatabase
import java.util.*

@Dao
interface MetaDao {
    @Query("SELECT * FROM meta WHERE created < :expireDate")
    fun expire(expireDate: Date):List<Meta>?

    @Query("DELETE FROM meta WHERE created < :expireDate")
    fun expireClean(expireDate: Date? = AppDatabase.getDefaultExpire())

    @Query("SELECT COUNT(*) FROM meta")
    fun count(): Int

    @Query("SELECT * FROM meta WHERE url = (:url)")
    fun get(url: String): Meta?

    @Query("SELECT * FROM meta")
    fun getAll(): List<Meta>?

    @Query("DELETE FROM meta WHERE 1")
    fun deleteAll()

    @Insert
    fun add(meta: Meta)
}