package cool.mixi.dica.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.User
import cool.mixi.dica.database.AppDatabase
import java.util.*

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(user: User)

    @Query("DELETE FROM user WHERE updatedAt < :expireDate")
    fun expireClean(expireDate: Date? = AppDatabase.getDefaultExpire(Consts.TTL_USER))

    @Query("SELECT * FROM user WHERE screen_name LIKE :name OR name LIKE :name")
    fun search(name: String): List<User>

    @Query("SELECT COUNT(*) FROM user")
    fun count(): Int

    @Query("SELECT * FROM user")
    fun getAll(): List<User>?

}