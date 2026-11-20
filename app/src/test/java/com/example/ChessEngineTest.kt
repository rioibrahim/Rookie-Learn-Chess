package com.example

import com.example.chess.AI_LEVELS
import com.example.chess.ChessAi
import com.example.chess.ChessGame
import com.example.chess.GameStatus
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.content.RookieCurriculum
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ChessEngineTest {

  @Test
  fun startingPositionHas20LegalMoves() {
    val game = ChessGame()
    assertEquals(20, game.legalMoves().size)
    assertEquals(ChessGame.STARTING_FEN, game.fen())
  }

  @Test
  fun castlingEnPassantAndPromotionWork() {
    // En passant
    val epGame = ChessGame("4k3/8/8/3pP3/8/8/8/4K3 w - d6 0 1")
    val epMove = epGame.makeMove("e5", "d6")
    assertNotNull(epMove)
    assertTrue(epMove!!.isEnPassant)
    assertEquals("exd6", epMove.san)
    assertEquals(null, epGame.pieceAt("d5"))

    // Promotion
    val promoGame = ChessGame("4k3/P7/8/8/8/8/8/4K3 w - - 0 1")
    val promoMove = promoGame.makeMove("a7", "a8", PieceType.QUEEN)
    assertNotNull(promoMove)
    assertEquals(PieceType.QUEEN, promoMove!!.promotion)
    assertEquals("a8=Q+", promoMove.san)

    // Kingside Castling
    val castleGame = ChessGame("r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1")
    val castleMove = castleGame.makeMove("e1", "g1")
    assertNotNull(castleMove)
    assertTrue(castleMove!!.isKingsideCastle)
    assertEquals("O-O", castleMove.san)
    assertEquals(PieceType.ROOK, castleGame.pieceAt("f1")?.type)
  }

  @Test
  fun allLessonStepsAreLegalAndValid() {
    assertEquals(6, RookieCurriculum.lessons.size)
    assertEquals(13, RookieCurriculum.totalLessonSteps)
    RookieCurriculum.lessons.forEach { lesson ->
      lesson.steps.forEach { step ->
        val game = ChessGame(step.fen)
        val move = game.makeMove(step.bestFrom, step.bestTo, PieceType.QUEEN)
        assertNotNull("Best move ${step.bestUci} should be legal in ${lesson.title}", move)
        assertTrue(step.bestTo in step.acceptedDestinations)
      }
    }
  }

  @Test
  fun allVerifiedPuzzlesHaveValidSolutions() {
    assertEquals(7, RookieCurriculum.puzzles.size)
    RookieCurriculum.puzzles.forEach { puzzle ->
      val game = ChessGame(puzzle.fen)
      if (puzzle.solutionUci != null) {
        val from = puzzle.solutionUci.substring(0, 2)
        val to = puzzle.solutionUci.substring(2, 4)
        val move = game.makeMove(from, to)
        assertNotNull("Fork puzzle solution should be legal", move)
      } else {
        val mate = puzzle.mateMove()
        assertNotNull("Mate-in-1 puzzle #${puzzle.index} must have a checkmate move", mate)
        val played = game.makeMove(mate!!.from, mate.to, mate.promotion)
        assertNotNull(played)
        assertTrue(game.isCheckmate())
        assertEquals(GameStatus.Checkmate(played!!.piece.color), game.status())
      }
    }
  }

  @Test
  fun aiFindsLegalMoveAndCapturesFreeQueen() = runBlocking {
    val game = ChessGame("4k3/8/8/3q4/8/8/3R4/4K3 w - - 0 1")
    val move = ChessAi.pickMove(game, AI_LEVELS[2], budgetMillis = 500L)
    assertNotNull(move)
    assertEquals("d2", move!!.fromSq)
    assertEquals("d5", move.toSq)
    assertFalse(game.isCheckmate())
    assertEquals(PieceColor.WHITE, game.turn)
  }
}
