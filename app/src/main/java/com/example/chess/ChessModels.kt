package com.example.chess

enum class PieceColor {
  WHITE,
  BLACK;

  fun opposite(): PieceColor = if (this == WHITE) BLACK else WHITE

  fun fenChar(): Char = if (this == WHITE) 'w' else 'b'

  fun displayName(): String = if (this == WHITE) "White" else "Black"
}

enum class PieceType(val symbol: Char, val sanLetter: String, val displayName: String, val value: Int) {
  PAWN('p', "", "pawn", 100),
  KNIGHT('n', "N", "knight", 320),
  BISHOP('b', "B", "bishop", 330),
  ROOK('r', "R", "rook", 500),
  QUEEN('q', "Q", "queen", 900),
  KING('k', "K", "king", 0);

  companion object {
    fun fromChar(c: Char): PieceType? = entries.find { it.symbol == c.lowercaseChar() }
  }
}

data class Piece(val type: PieceType, val color: PieceColor) {
  fun fenChar(): Char =
    if (color == PieceColor.WHITE) type.symbol.uppercaseChar() else type.symbol.lowercaseChar()

  companion object {
    fun fromFenChar(c: Char): Piece? {
      val type = PieceType.fromChar(c) ?: return null
      val color = if (c.isUpperCase()) PieceColor.WHITE else PieceColor.BLACK
      return Piece(type, color)
    }
  }
}

data class CastlingRights(
  val whiteKingside: Boolean = true,
  val whiteQueenside: Boolean = true,
  val blackKingside: Boolean = true,
  val blackQueenside: Boolean = true,
) {
  fun toMask(): Int =
    (if (whiteKingside) 1 else 0) or
      (if (whiteQueenside) 2 else 0) or
      (if (blackKingside) 4 else 0) or
      (if (blackQueenside) 8 else 0)

  fun toFen(): String {
    val sb = StringBuilder()
    if (whiteKingside) sb.append('K')
    if (whiteQueenside) sb.append('Q')
    if (blackKingside) sb.append('k')
    if (blackQueenside) sb.append('q')
    return if (sb.isEmpty()) "-" else sb.toString()
  }

  companion object {
    fun fromMask(mask: Int): CastlingRights = CastlingRights(
      whiteKingside = (mask and 1) != 0,
      whiteQueenside = (mask and 2) != 0,
      blackKingside = (mask and 4) != 0,
      blackQueenside = (mask and 8) != 0,
    )

    fun fromFen(token: String): CastlingRights {
      if (token == "-") return CastlingRights(false, false, false, false)
      return CastlingRights(
        whiteKingside = 'K' in token,
        whiteQueenside = 'Q' in token,
        blackKingside = 'k' in token,
        blackQueenside = 'q' in token,
      )
    }
  }
}

data class ChessMove(
  val from: Int,
  val to: Int,
  val piece: Piece,
  val captured: Piece? = null,
  val promotion: PieceType? = null,
  val isEnPassant: Boolean = false,
  val isKingsideCastle: Boolean = false,
  val isQueensideCastle: Boolean = false,
  val san: String = "",
) {
  val fromSq: String get() = squareName(from)
  val toSq: String get() = squareName(to)
  val uci: String get() = fromSq + toSq + (promotion?.symbol ?: "")
  val isCastle: Boolean get() = isKingsideCastle || isQueensideCastle
}

enum class MoveSoundKind {
  MOVE,
  CAPTURE,
  CHECK,
  CHECKMATE,
  CASTLE,
  PROMOTE,
}

sealed interface GameStatus {
  data object Ongoing : GameStatus
  data class Checkmate(val winner: PieceColor) : GameStatus
  data object Stalemate : GameStatus
  data object RepetitionDraw : GameStatus
  data object InsufficientMaterialDraw : GameStatus
  data object FiftyMoveDraw : GameStatus

  val isOver: Boolean get() = this !is Ongoing

  fun description(): String = when (this) {
    Ongoing -> ""
    is Checkmate -> "Checkmate — ${winner.displayName()} wins"
    Stalemate -> "Stalemate — draw"
    RepetitionDraw -> "Draw by repetition"
    InsufficientMaterialDraw -> "Draw — insufficient material"
    FiftyMoveDraw -> "Draw — 50-move rule"
  }
}

data class AiLevel(
  val name: String,
  val depth: Int,
  val randomness: Int,
  val minSeconds: Double,
  val maxSeconds: Double,
)

// Tuned for crisp, native mobile responsiveness without artificial multi-second lag
val AI_LEVELS = listOf(
  AiLevel("Beginner", depth = 1, randomness = 500, minSeconds = 0.22, maxSeconds = 0.45),
  AiLevel("Easy", depth = 1, randomness = 60, minSeconds = 0.22, maxSeconds = 0.48),
  AiLevel("Casual", depth = 2, randomness = 30, minSeconds = 0.25, maxSeconds = 0.55),
  AiLevel("Intermediate", depth = 2, randomness = 10, minSeconds = 0.25, maxSeconds = 0.60),
  AiLevel("Advanced", depth = 3, randomness = 0, minSeconds = 0.28, maxSeconds = 0.75),
  AiLevel("Expert", depth = 3, randomness = 0, minSeconds = 0.28, maxSeconds = 0.90),
)

fun squareIndex(name: String): Int {
  require(name.length == 2) { "Invalid square: $name" }
  val col = name[0] - 'a'
  val row = 8 - (name[1] - '0')
  return row * 8 + col
}

fun squareName(index: Int): String {
  val row = index / 8
  val col = index % 8
  return "${('a' + col)}${8 - row}"
}

fun rowOf(index: Int): Int = index / 8
fun colOf(index: Int): Int = index % 8
