package com.example.content

import com.example.chess.ChessGame
import com.example.chess.ChessMove

data class LessonStep(
  val fen: String,
  val prompt: String,
  val bestUci: String,
  val acceptedDestinations: Set<String>,
  val idea: String,
  val why: String,
) {
  val bestFrom: String get() = bestUci.substring(0, 2)
  val bestTo: String get() = bestUci.substring(2, 4)
}

data class Lesson(
  val title: String,
  val steps: List<LessonStep>,
)

data class ChessPuzzle(
  val index: Int,
  val category: String,
  val fen: String,
  val prompt: String,
  val solutionUci: String? = null,
  val why: String? = null,
) {
  fun mateMove(): ChessMove? {
    val game = ChessGame(fen)
    return game.legalMoves().find { move ->
      game.makeMoveFast(move)
      val mate = game.isCheckmate()
      game.undoMove()
      mate
    }
  }
}

object RookieCurriculum {

  private const val KING_ONLY_FEN = "4k3/8/8/8/8/8/8/4K3 w - - 0 1"

  val lessons: List<Lesson> = listOf(
    Lesson(
      title = "Pawn",
      steps = listOf(
        LessonStep(
          fen = "4k3/8/8/8/8/8/4P3/4K3 w - - 0 1",
          prompt = "Pawns march forward only. From their starting square they may advance one OR two squares. Move the e-pawn two squares.",
          bestUci = "e2e4",
          acceptedDestinations = setOf("e3", "e4"),
          idea = "A pawn never goes backward or sideways. On its first move it can take a double step.",
          why = "e2–e4 is a double step, only allowed on a pawn’s first move. It also claims the centre.",
        ),
        LessonStep(
          fen = "4k3/8/8/3p4/4P3/8/8/4K3 w - - 0 1",
          prompt = "Pawns move straight but capture diagonally. Capture the black pawn.",
          bestUci = "e4d5",
          acceptedDestinations = setOf("d5"),
          idea = "Pawns capture one square diagonally forward, not straight ahead.",
          why = "exd5: the pawn captured diagonally. Note the notation: “e” = file the pawn left, “x” = capture, “d5” = destination.",
        ),
        LessonStep(
          fen = "4k3/P7/8/8/8/8/8/4K3 w - - 0 1",
          prompt = "A pawn that reaches the far rank is promoted — usually to a queen. Push the pawn to promote.",
          bestUci = "a7a8",
          acceptedDestinations = setOf("a8"),
          idea = "This pawn is one step from the last rank.",
          why = "a8=Q: promotion turns a humble pawn into the strongest piece. (You choose the new piece; a queen is usually best.)",
        ),
      ),
    ),
    Lesson(
      title = "Rook",
      steps = listOf(
        LessonStep(
          fen = "4k3/8/8/8/8/8/8/R3K3 w - - 0 1",
          prompt = "Rooks slide any number of squares along files (columns) and ranks (rows). Move the rook to the far end of the a-file.",
          bestUci = "a1a8",
          acceptedDestinations = setOf("a8"),
          idea = "Rooks move in straight lines, as far as nothing blocks them.",
          why = "Ra8+ slid up the open file, and gives check too.",
        ),
        LessonStep(
          fen = "4k3/8/8/3p4/8/8/8/3RK3 w - - 0 1",
          prompt = "Rooks capture the first enemy piece in their path. Capture the pawn on d5.",
          bestUci = "d1d5",
          acceptedDestinations = setOf("d5"),
          idea = "Look straight up the d-file.",
          why = "Rxd5: the rook slid up the file and captured. Rooks are strongest on open files.",
        ),
      ),
    ),
    Lesson(
      title = "Bishop",
      steps = listOf(
        LessonStep(
          fen = "4k3/8/8/8/8/8/8/2B1K3 w - - 0 1",
          prompt = "Bishops slide diagonally, so they stay on one square colour forever. Move the bishop to h6.",
          bestUci = "c1h6",
          acceptedDestinations = setOf("h6"),
          idea = "Follow the long diagonal up and to the right.",
          why = "Bc1–h6 slid along a single diagonal. A light-squared bishop can never reach a dark square.",
        ),
      ),
    ),
    Lesson(
      title = "Knight",
      steps = listOf(
        LessonStep(
          fen = "4k3/8/8/8/3N4/8/8/4K3 w - - 0 1",
          prompt = "Knights move in an “L”: two squares one way, then one square sideways. Move the knight to f5.",
          bestUci = "d4f5",
          acceptedDestinations = setOf("f5"),
          idea = "Count two squares along a rank, then one along a file.",
          why = "d4→f5 is two right and one up: an L shape. Knights from the centre control eight squares.",
        ),
        LessonStep(
          fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1",
          prompt = "Knights are the only pieces that JUMP over others. Develop the g1 knight to f3.",
          bestUci = "g1f3",
          acceptedDestinations = setOf("f3", "h3"),
          idea = "Your knight can leap over the pawn in front of it.",
          why = "Nf3 jumps the pawn wall, develops a piece and eyes the centre (d4/e5). Developing knights early is a core principle.",
        ),
      ),
    ),
    Lesson(
      title = "Queen",
      steps = listOf(
        LessonStep(
          fen = "4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1",
          prompt = "The queen = rook + bishop. Move it diagonally to a7.",
          bestUci = "d4a7",
          acceptedDestinations = setOf("a7"),
          idea = "Use the bishop-like part of the queen.",
          why = "Qa7 moved diagonally. Early in games, don’t rush your queen out — she can be chased by lesser pieces.",
        ),
        LessonStep(
          fen = "4k3/8/8/8/3Q4/8/8/4K3 w - - 0 1",
          prompt = "Now use the rook-like move: slide the queen sideways to h4.",
          bestUci = "d4h4",
          acceptedDestinations = setOf("h4"),
          idea = "Straight along the 4th rank.",
          why = "Qh4: same queen, rook-style move. She controls up to 27 squares from an open centre.",
        ),
      ),
    ),
    Lesson(
      title = "King",
      steps = listOf(
        LessonStep(
          fen = KING_ONLY_FEN,
          prompt = "The king moves one square in any direction (but never into check). Move the king to e2.",
          bestUci = "e1e2",
          acceptedDestinations = setOf("d1", "d2", "e2", "f2", "f1"),
          idea = "Just one square — any direction.",
          why = "The king takes single steps. Kings must never move to an attacked square.",
        ),
        LessonStep(
          fen = "4k3/8/8/8/8/8/4r3/4K3 w - - 0 1",
          prompt = "Your king is in CHECK (attacked). You must escape: capture the rook!",
          bestUci = "e1e2",
          acceptedDestinations = setOf("e2", "d1", "f1"),
          idea = "The rook is undefended and right next to your king.",
          why = "Kxe2 removed the attacker. Ways out of check: move the king, capture the attacker, or block.",
        ),
        LessonStep(
          fen = "r3k2r/8/8/8/8/8/8/R3K2R w KQkq - 0 1",
          prompt = "Castling: king moves two squares toward a rook and the rook jumps over. Castle kingside.",
          bestUci = "e1g1",
          acceptedDestinations = setOf("g1", "c1"),
          idea = "Move the KING two squares toward a rook.",
          why = "O-O tucks the king away and brings the rook into play, in one move.",
        ),
      ),
    ),
  )

  val totalLessonSteps: Int = lessons.sumOf { it.steps.size }

  val puzzles: List<ChessPuzzle> = listOf(
    ChessPuzzle(
      index = 0,
      category = "Mate in 1",
      fen = "6k1/5ppp/8/8/8/8/8/R3K3 w - - 0 1",
      prompt = "Back-rank mate: the king is boxed in by its own pawns.",
    ),
    ChessPuzzle(
      index = 1,
      category = "Mate in 1",
      fen = "7k/8/6K1/8/8/8/8/5Q2 w - - 0 1",
      prompt = "Queen and king cooperate.",
    ),
    ChessPuzzle(
      index = 2,
      category = "Mate in 1",
      fen = "r1bqkb1r/pppp1ppp/2n2n2/4p2Q/2B1P3/8/PPPP1PPP/RNB1K1NR w KQkq - 4 4",
      prompt = "The famous Scholar’s Mate pattern.",
    ),
    ChessPuzzle(
      index = 3,
      category = "Mate in 1",
      fen = "6rk/6pp/8/6N1/8/8/8/6K1 w - - 0 1",
      prompt = "Smothered mate: the king is trapped by its own pieces.",
    ),
    ChessPuzzle(
      index = 4,
      category = "Mate in 1",
      fen = "k7/8/1K6/8/8/8/8/7R w - - 0 1",
      prompt = "Use your king to cut off escape squares.",
    ),
    ChessPuzzle(
      index = 5,
      category = "Mate in 1",
      fen = "4r1k1/8/8/8/8/8/5PPP/6K1 b - - 0 1",
      prompt = "Black to move — same back-rank idea.",
    ),
    ChessPuzzle(
      index = 6,
      category = "Forks",
      fen = "r3k3/8/8/3N4/8/8/8/4K3 w - - 0 1",
      prompt = "Find a knight move that attacks the king AND the rook.",
      solutionUci = "d5c7",
      why = "Nc7+ forks king and rook; the king must move, then Nxa8 wins the rook.",
    ),
  )

  fun lessonStepKey(lessonIndex: Int, stepIndex: Int): String = "$lessonIndex-$stepIndex"
}
