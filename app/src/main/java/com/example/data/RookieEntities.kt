package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lesson_completions")
data class LessonCompletionEntity(
  @PrimaryKey val stepKey: String,
  val lessonIndex: Int,
  val stepIndex: Int,
  val completedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "puzzle_stats")
data class PuzzleStatsEntity(
  @PrimaryKey val id: Int = 1,
  val solved: Int = 0,
  val attempted: Int = 0,
  val rating: Int = 800,
  val streak: Int = 0,
)

@Entity(tableName = "puzzle_categories")
data class PuzzleCategoryEntity(
  @PrimaryKey val category: String,
  val attempted: Int = 0,
  val solved: Int = 0,
)

@Entity(tableName = "solved_puzzles")
data class SolvedPuzzleEntity(
  @PrimaryKey val puzzleIndex: Int,
  val solvedAtMillis: Long = System.currentTimeMillis(),
)

@Entity(tableName = "game_records")
data class GameRecordEntity(
  @PrimaryKey val id: Int = 1,
  val wins: Int = 0,
  val draws: Int = 0,
  val losses: Int = 0,
)
