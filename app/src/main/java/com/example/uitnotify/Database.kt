package com.example.uitnotify

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import android.content.Context

@Entity(tableName = "articles")
data class Article(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    @ColumnInfo(name = "header") val header: String,
    @ColumnInfo(name = "date") val date: String,
    @ColumnInfo(name = "content") val content: String,
    @ColumnInfo(name = "url") val url: String
)

@Dao
interface ArticleDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertArticles(articles: Article)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAllArticles(articles: List<Article>)

    @Query("SELECT * FROM articles")
    suspend fun getAllArticles(): List<Article>
}

@Database(entities = [Article::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun articleDao(): ArticleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "app_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}