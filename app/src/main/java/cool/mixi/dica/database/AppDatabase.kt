package cool.mixi.dica.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import cool.mixi.dica.App
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.Meta
import cool.mixi.dica.bean.User
import cool.mixi.dica.database.dao.MetaDao
import cool.mixi.dica.database.dao.UserDao
import cool.mixi.dica.util.TimestampConverter
import java.util.*

@Database(entities = [Meta::class, User::class], version = 2, exportSchema = false)
@TypeConverters(TimestampConverter::class)
abstract class AppDatabase: RoomDatabase() {

    abstract fun metaDao(): MetaDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var db: AppDatabase? = null

        fun getInstance(): AppDatabase {
            return db ?: synchronized(this) {
                db = Room.databaseBuilder(
                    App.instance.applicationContext,
                    AppDatabase::class.java,
                    Consts.DB_NAME
                )
                    .fallbackToDestructiveMigration()
                    .build()
                db!!
            }
        }

        fun getDefaultExpire(expire: Int): Date {
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.MINUTE, 0 - expire)
            return calendar.time
        }
    }
}