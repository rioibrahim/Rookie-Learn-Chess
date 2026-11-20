package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
  entities = [
    LessonCompletionEntity::class,
    PuzzleStatsEntity::class,
    PuzzleCategoryEntity::class,
    SolvedPuzzleEntity::class,
    GameRecordEntity::class,
  ],
  version = 1,
  exportSchema = false,
)
abstract class RookieDatabase : RoomDatabase() {
  abstract fun rookieDao(): RookieDao

  companion object {
    @Volatile
    private var INSTANCE: RookieDatabase? = null

    fun getInstance(context: Context): RookieDatabase {
      return INSTANCE ?: synchronized(this) {
        INSTANCE ?: Room.databaseBuilder(
          context.applicationContext,
          RookieDatabase::class.java,
          "rookie_chess.db",
        ).build().also { INSTANCE = it }
      }
    }
  }
}
