package cool.mixi.dica.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import cool.mixi.dica.App
import cool.mixi.dica.bean.Consts
import cool.mixi.dica.bean.HashTag
import cool.mixi.dica.bean.Meta
import cool.mixi.dica.bean.User
import cool.mixi.dica.database.dao.HashTagDao
import cool.mixi.dica.database.dao.MetaDao
import cool.mixi.dica.database.dao.UserDao
import cool.mixi.dica.util.TimestampConverter
import cool.mixi.dica.util.tagEscapeHash
import org.jetbrains.anko.doAsync
import java.util.*

@Database(entities = [Meta::class, User::class, HashTag::class], version = 3, exportSchema = false)
@TypeConverters(TimestampConverter::class)
abstract class AppDatabase: RoomDatabase() {

    abstract fun metaDao(): MetaDao
    abstract fun userDao(): UserDao
    abstract fun hashTagDao(): HashTagDao

    companion object {
        @Volatile
        private var db: AppDatabase? = null

        fun getInstance(): AppDatabase {
            return db ?: synchronized(this) {
                db = Room.databaseBuilder(
                    App.instance.applicationContext,
                    AppDatabase::class.java,
                    "${Consts.DB_NAME}01"
                )
                    .addMigrations(MigrationDb(2, 3))
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

        fun upsertHashTag(tag: String){
            doAsync {
                var tmpStr = tag.tagEscapeHash()
                val dao = AppDatabase.getInstance().hashTagDao()
                val hashTag = HashTag(tmpStr)
                dao.add(hashTag)
                dao.incr(tmpStr)
            }
        }
    }

    class MigrationDb(startVersion:Int, endVersion:Int): Migration(startVersion, endVersion) {
        override fun migrate(database: SupportSQLiteDatabase) {}
    }

}