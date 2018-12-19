package cool.mixi.dica.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.HashTag
import cool.mixi.dica.database.AppDatabase
import java.util.*

@Dao
interface HashTagDao {

    @Query("SELECT COUNT(*) FROM tag")
    fun count(): Int

    @Query("DELETE FROM tag WHERE updatedAt < :expireDate AND searchCount = 0")
    fun expireClean(expireDate: Date? = AppDatabase.getDefaultExpire(Consts.TTL_TAG))

    @Query("UPDATE tag SET searchCount = searchCount+1, updatedAt = :date WHERE name = :name")
    fun incr(name: String, date: Long? = Date().time)

    @Query("SELECT * FROM tag WHERE name = :name")
    fun get(name:String): HashTag?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun add(hashTag: HashTag)

    @Query("SELECT * FROM tag ORDER BY searchCount DESC")
    fun getAll(): List<HashTag>?
}