package com.example.chess

import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.coroutineContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random
import kotlinx.coroutines.ensureActive

data class SearchTelemetry(
  val depth: Int,
  val nodes: Int,
)

object ChessAi {

  private class SearchTimeoutException : RuntimeException()

  fun evaluate(game: ChessGame): Int {
    var score = 0
    val board = game.rawBoard()
    for (idx in 0 until 64) {
      val piece = board[idx] ?: continue
      val r = rowOf(idx)
      val c = colOf(idx)
      var v = piece.type.value
      if (piece.type == PieceType.PAWN) {
        v += (if (piece.color == PieceColor.WHITE) 6 - r else r - 1) * 6
      }
      if (c in 2..5 && r in 2..5 &&
        (piece.type == PieceType.PAWN || piece.type == PieceType.KNIGHT || piece.type == PieceType.BISHOP)
      ) {
        v += 10
      }
      score += if (piece.color == PieceColor.WHITE) v else -v
    }
    return score
  }

  fun complexity(game: ChessGame): Double {
    val moves = game.legalMovesFast()
    val n = moves.size
    if (n <= 1) return 0.0
    val captures = moves.count { it.captured != null }
    var s = min(1.0, n / 40.0) * 0.3 +
      min(1.0, captures / 6.0) * 0.2 +
      (if (game.inCheck()) 0.15 else 0.0)

    var whiteMat = 0
    var blackMat = 0
    var pieceCount = 0
    val board = game.rawBoard()
    for (idx in 0 until 64) {
      val p = board[idx] ?: continue
      pieceCount++
      if (p.type != PieceType.KING) {
        if (p.color == PieceColor.WHITE) whiteMat += p.type.value else blackMat += p.type.value
      }
    }
    s += min(1.0, abs(whiteMat - blackMat) / 300.0) * 0.1
    if (pieceCount in 13..29) s += 0.1

    val oppCaps = game.flipTurnCountOpponentCaptures()
    s += min(1.0, oppCaps / 4.0) * 0.15
    return min(1.0, s)
  }

  fun thinkMillis(game: ChessGame, level: AiLevel): Long {
    val moveCount = game.legalMovesFast().size
    val sec = if (moveCount <= 1) {
      max(0.16, level.minSeconds * 0.6)
    } else {
      level.minSeconds + (level.maxSeconds - level.minSeconds) * complexity(game)
    }
    return (sec * 1000.0).toLong()
  }

  suspend fun pickMove(
    sourceGame: ChessGame,
    level: AiLevel,
    budgetMillis: Long,
    onProgress: (SearchTelemetry) -> Unit = {},
  ): ChessMove? {
    val game = sourceGame.copy()
    val allInitial = game.legalMoves()
    if (allInitial.isEmpty()) return null
    if (allInitial.size == 1) {
      onProgress(SearchTelemetry(depth = 1, nodes = 1))
      return allInitial[0]
    }

    val deadlineNanos = System.nanoTime() + budgetMillis * 1_000_000L
    val maximizing = game.turn == PieceColor.WHITE
    var bestMove = allInitial[0]
    var orderedMoves = orderMovesForSearch(allInitial)
    val counter = intArrayOf(0)

    for (d in 1..level.depth) {
      coroutineContext.ensureActive()
      try {
        val scored = ArrayList<Pair<ChessMove, Double>>(orderedMoves.size)
        for (i in 0 until orderedMoves.size) {
          coroutineContext.ensureActive()
          val m = orderedMoves[i]
          game.makeSearchMove(m)
          val value = try {
            alphaBeta(game, d - 1, -1e9, 1e9, deadlineNanos, counter)
          } finally {
            game.undoSearchMove()
          }
          scored.add(m to (value + Random.nextDouble() * level.randomness))
        }
        scored.sortWith { a, b ->
          if (maximizing) b.second.compareTo(a.second) else a.second.compareTo(b.second)
        }
        bestMove = scored[0].first
        orderedMoves = scored.map { it.first }
        onProgress(SearchTelemetry(depth = d, nodes = counter[0]))
      } catch (_: SearchTimeoutException) {
        break
      } catch (ce: CancellationException) {
        throw ce
      }
    }
    return bestMove
  }

  fun pickHintMove(sourceGame: ChessGame): ChessMove? {
    val game = sourceGame.copy()
    val moves = game.legalMoves()
    if (moves.isEmpty()) return null
    val maximizing = game.turn == PieceColor.WHITE
    val counter = intArrayOf(0)
    val deadline = System.nanoTime() + 900_000_000L
    var best = moves[0]
    var bestVal = if (maximizing) -1e9 else 1e9
    val ordered = orderMovesForSearch(moves)
    for (i in 0 until ordered.size) {
      val m = ordered[i]
      game.makeSearchMove(m)
      val v = try {
        alphaBeta(game, 1, -1e9, 1e9, deadline, counter)
      } catch (_: SearchTimeoutException) {
        evaluate(game).toDouble()
      } finally {
        game.undoSearchMove()
      }
      if ((maximizing && v > bestVal) || (!maximizing && v < bestVal)) {
        bestVal = v
        best = m
      }
    }
    return best
  }

  private fun orderMovesForSearch(moves: List<ChessMove>): List<ChessMove> {
    if (moves.size <= 1) return moves
    val ordered = ArrayList<ChessMove>(moves.size)
    // Captures first (MVV-LVA lightweight pass), then quiet moves
    for (i in 0 until moves.size) {
      val m = moves[i]
      if (m.captured != null) ordered.add(m)
    }
    for (i in 0 until moves.size) {
      val m = moves[i]
      if (m.captured == null) ordered.add(m)
    }
    return ordered
  }

  private fun alphaBeta(
    game: ChessGame,
    depth: Int,
    alphaInit: Double,
    betaInit: Double,
    deadlineNanos: Long,
    counter: IntArray,
  ): Double {
    counter[0]++
    if ((counter[0] and 127) == 0 && System.nanoTime() > deadlineNanos) {
      throw SearchTimeoutException()
    }
    val whiteTurn = game.turn == PieceColor.WHITE
    val moves = game.legalMovesFast()
    if (moves.isEmpty()) {
      return if (game.inCheck()) {
        if (whiteTurn) -100000.0 - depth else 100000.0 + depth
      } else {
        0.0
      }
    }
    if (game.isInsufficientMaterial() || game.isFiftyMoveDraw()) {
      return 0.0
    }
    if (depth <= 0) return evaluate(game).toDouble()

    var alpha = alphaInit
    var beta = betaInit
    var best = if (whiteTurn) -1e9 else 1e9

    // Two-pass iteration (captures first, then quiet moves) with zero list allocation!
    for (pass in 0..1) {
      val wantCapture = pass == 0
      for (i in 0 until moves.size) {
        val m = moves[i]
        if ((m.captured != null) != wantCapture) continue
        game.makeSearchMove(m)
        val v = try {
          alphaBeta(game, depth - 1, alpha, beta, deadlineNanos, counter)
        } finally {
          game.undoSearchMove()
        }
        if (whiteTurn) {
          if (v > best) best = v
          if (best > alpha) alpha = best
        } else {
          if (v < best) best = v
          if (best < beta) beta = best
        }
        if (beta <= alpha) return best
      }
    }
    return best
  }
}
