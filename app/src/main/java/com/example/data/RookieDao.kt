package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RookieDao {
  @Query("SELECT * FROM lesson_completions")
  fun observeCompletedLessons(): Flow<List<LessonCompletionEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun markLessonStepDone(entity: LessonCompletionEntity)

  @Query("SELECT * FROM puzzle_stats WHERE id = 1")
  fun observePuzzleStats(): Flow<PuzzleStatsEntity?>

  @Query("SELECT * FROM puzzle_stats WHERE id = 1")
  suspend fun getPuzzleStats(): PuzzleStatsEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPuzzleStats(stats: PuzzleStatsEntity)

  @Query("SELECT * FROM puzzle_categories")
  fun observePuzzleCategories(): Flow<List<PuzzleCategoryEntity>>

  @Query("SELECT * FROM puzzle_categories WHERE category = :category")
  suspend fun getPuzzleCategory(category: String): PuzzleCategoryEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPuzzleCategory(entity: PuzzleCategoryEntity)

  @Query("SELECT * FROM solved_puzzles")
  fun observeSolvedPuzzles(): Flow<List<SolvedPuzzleEntity>>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun markPuzzleSolved(entity: SolvedPuzzleEntity)

  @Query("SELECT * FROM game_records WHERE id = 1")
  fun observeGameRecord(): Flow<GameRecordEntity?>

  @Query("SELECT * FROM game_records WHERE id = 1")
  suspend fun getGameRecord(): GameRecordEntity?

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertGameRecord(record: GameRecordEntity)
}
