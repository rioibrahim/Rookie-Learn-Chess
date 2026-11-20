package com.example.chess

import kotlin.math.abs

class ChessGame(fen: String = STARTING_FEN) {

  private data class UndoState(
    val castlingMask: Int,
    val enPassantSquare: Int?,
    val halfMoveClock: Int,
    val fullMoveNumber: Int,
    val move: ChessMove,
    val zobristHash: Long,
  )

  private var board: Array<Piece?> = arrayOfNulls(64)
  var turn: PieceColor = PieceColor.WHITE
    private set
  var castling: CastlingRights = CastlingRights()
    private set
  var enPassantSquare: Int? = null
    private set
  var halfMoveClock: Int = 0
    private set
  var fullMoveNumber: Int = 1
    private set

  private var whiteKingSq: Int = -1
  private var blackKingSq: Int = -1
  private var zobristHash: Long = 0L

  private val undoStack = mutableListOf<UndoState>()
  private val positionCounts = HashMap<Long, Int>(64)
  private var cachedLegalMoves: List<ChessMove>? = null

  init {
    loadFen(fen)
  }

  fun copy(): ChessGame {
    val clone = ChessGame(STARTING_FEN)
    System.arraycopy(this.board, 0, clone.board, 0, 64)
    clone.turn = this.turn
    clone.castling = this.castling
    clone.enPassantSquare = this.enPassantSquare
    clone.halfMoveClock = this.halfMoveClock
    clone.fullMoveNumber = this.fullMoveNumber
    clone.whiteKingSq = this.whiteKingSq
    clone.blackKingSq = this.blackKingSq
    clone.zobristHash = this.zobristHash
    clone.undoStack.clear()
    clone.positionCounts.clear()
    clone.positionCounts.putAll(this.positionCounts)
    clone.cachedLegalMoves = null
    return clone
  }

  fun loadFen(fen: String) {
    val parts = fen.trim().split(Regex("\\s+"))
    require(parts.size >= 4) { "Invalid FEN: $fen" }
    board = arrayOfNulls(64)
    whiteKingSq = -1
    blackKingSq = -1
    val rows = parts[0].split('/')
    require(rows.size == 8) { "Invalid FEN board rows: $fen" }
    for (r in 0 until 8) {
      var c = 0
      for (ch in rows[r]) {
        if (ch.isDigit()) {
          c += ch.digitToInt()
        } else {
          val piece = Piece.fromFenChar(ch) ?: error("Invalid FEN piece: $ch")
          val idx = r * 8 + c
          board[idx] = piece
          if (piece.type == PieceType.KING) {
            if (piece.color == PieceColor.WHITE) whiteKingSq = idx else blackKingSq = idx
          }
          c++
        }
      }
    }
    turn = if (parts[1] == "b") PieceColor.BLACK else PieceColor.WHITE
    castling = CastlingRights.fromFen(parts[2])
    enPassantSquare = if (parts[3] == "-") null else squareIndex(parts[3])
    halfMoveClock = parts.getOrNull(4)?.toIntOrNull() ?: 0
    fullMoveNumber = parts.getOrNull(5)?.toIntOrNull() ?: 1
    undoStack.clear()
    positionCounts.clear()
    zobristHash = computeZobristHash()
    positionCounts[zobristHash] = 1
    cachedLegalMoves = null
  }

  fun pieceAt(index: Int): Piece? = board[index]
  fun pieceAt(square: String): Piece? = board[squareIndex(square)]

  fun boardSnapshot(): List<Piece?> = board.toList()

  internal fun rawBoard(): Array<Piece?> = board

  fun history(): List<ChessMove> = undoStack.map { it.move }

  fun lastMove(): ChessMove? = undoStack.lastOrNull()?.move

  fun fen(): String {
    val sb = StringBuilder(72)
    for (r in 0 until 8) {
      var empty = 0
      for (c in 0 until 8) {
        val p = board[r * 8 + c]
        if (p == null) {
          empty++
        } else {
          if (empty > 0) {
            sb.append(empty)
            empty = 0
          }
          sb.append(p.fenChar())
        }
      }
      if (empty > 0) sb.append(empty)
      if (r < 7) sb.append('/')
    }
    sb.append(' ').append(turn.fenChar())
    sb.append(' ').append(castling.toFen())
    sb.append(' ').append(enPassantSquare?.let(::squareName) ?: "-")
    sb.append(' ').append(halfMoveClock)
    sb.append(' ').append(fullMoveNumber)
    return sb.toString()
  }

  fun pgn(): String {
    if (undoStack.isEmpty()) return ""
    val sb = StringBuilder(undoStack.size * 7)
    undoStack.forEachIndexed { idx, entry ->
      if (idx % 2 == 0) {
        if (idx > 0) sb.append(' ')
        sb.append("${idx / 2 + 1}. ")
      } else {
        sb.append(' ')
      }
      sb.append(entry.move.san)
    }
    return sb.toString()
  }

  private fun computeZobristHash(): Long {
    var h = 0L
    for (i in 0 until 64) {
      val p = board[i] ?: continue
      val pieceIdx = p.type.ordinal + (if (p.color == PieceColor.BLACK) 6 else 0)
      h = h xor ZOBRIST_PIECES[pieceIdx * 64 + i]
    }
    if (turn == PieceColor.BLACK) h = h xor ZOBRIST_SIDE
    h = h xor ZOBRIST_CASTLING[castling.toMask()]
    val ep = enPassantSquare
    if (ep != null) h = h xor ZOBRIST_EP[colOf(ep)]
    return h
  }

  fun kingSquare(color: PieceColor): Int =
    if (color == PieceColor.WHITE) whiteKingSq else blackKingSq

  fun inCheck(color: PieceColor = turn): Boolean {
    val kSq = kingSquare(color)
    if (kSq < 0) return false
    return isSquareAttackedBy(kSq, color.opposite())
  }

  fun isSquareAttackedBy(square: Int, attacker: PieceColor): Boolean {
    val r = rowOf(square)
    val c = colOf(square)

    val pawnRow = if (attacker == PieceColor.WHITE) r + 1 else r - 1
    if (pawnRow in 0..7) {
      if (c > 0) {
        val p = board[pawnRow * 8 + c - 1]
        if (p != null && p.color == attacker && p.type == PieceType.PAWN) return true
      }
      if (c < 7) {
        val p = board[pawnRow * 8 + c + 1]
        if (p != null && p.color == attacker && p.type == PieceType.PAWN) return true
      }
    }

    for (offset in KNIGHT_OFFSETS) {
      val nr = r + offset[0]
      val nc = c + offset[1]
      if (nr in 0..7 && nc in 0..7) {
        val p = board[nr * 8 + nc]
        if (p != null && p.color == attacker && p.type == PieceType.KNIGHT) return true
      }
    }

    for (dr in -1..1) {
      for (dc in -1..1) {
        if (dr == 0 && dc == 0) continue
        val kr = r + dr
        val kc = c + dc
        if (kr in 0..7 && kc in 0..7) {
          val p = board[kr * 8 + kc]
          if (p != null && p.color == attacker && p.type == PieceType.KING) return true
        }
      }
    }

    for (dir in ORTHO_DIRS) {
      var cr = r + dir[0]
      var cc = c + dir[1]
      while (cr in 0..7 && cc in 0..7) {
        val p = board[cr * 8 + cc]
        if (p != null) {
          if (p.color == attacker && (p.type == PieceType.ROOK || p.type == PieceType.QUEEN)) {
            return true
          }
          break
        }
        cr += dir[0]
        cc += dir[1]
      }
    }

    for (dir in DIAG_DIRS) {
      var cr = r + dir[0]
      var cc = c + dir[1]
      while (cr in 0..7 && cc in 0..7) {
        val p = board[cr * 8 + cc]
        if (p != null) {
          if (p.color == attacker && (p.type == PieceType.BISHOP || p.type == PieceType.QUEEN)) {
            return true
          }
          break
        }
        cr += dir[0]
        cc += dir[1]
      }
    }

    return false
  }

  fun legalMoves(square: Int? = null): List<ChessMove> {
    val all = cachedLegalMoves ?: computeLegalMoves(withSan = true).also { cachedLegalMoves = it }
    return if (square == null) all else all.filter { it.from == square }
  }

  fun legalMovesFast(): List<ChessMove> =
    cachedLegalMoves ?: computeLegalMoves(withSan = false)

  private fun computeLegalMoves(withSan: Boolean): List<ChessMove> {
    val pseudo = generatePseudoMoves()
    val legal = ArrayList<ChessMove>(pseudo.size)
    val movingSide = turn
    for (i in 0 until pseudo.size) {
      val m = pseudo[i]
      applyRawMove(m)
      val kingSafe = !inCheck(movingSide)
      if (kingSafe) {
        if (withSan) {
          val san = buildSan(m, pseudo)
          legal.add(m.copy(san = san))
        } else {
          legal.add(m)
        }
      }
      revertRawMove()
    }
    return legal
  }

  private fun buildSan(move: ChessMove, pseudo: List<ChessMove>): String {
    val base = buildString {
      when {
        move.isKingsideCastle -> append("O-O")
        move.isQueensideCastle -> append("O-O-O")
        move.piece.type == PieceType.PAWN -> {
          if (move.captured != null) {
            append(('a' + colOf(move.from)))
            append('x')
          }
          append(move.toSq)
          if (move.promotion != null) {
            append('=').append(move.promotion.sanLetter)
          }
        }
        else -> {
          append(move.piece.type.sanLetter)
          val movingSide = move.piece.color
          val others = pseudo.filter { cand ->
            cand.from != move.from &&
              cand.to == move.to &&
              cand.piece.type == move.piece.type &&
              cand.piece.color == movingSide
          }.filter { cand ->
            revertRawMove()
            applyRawMove(cand)
            val ok = !inCheck(movingSide)
            revertRawMove()
            applyRawMove(move)
            ok
          }
          if (others.isNotEmpty()) {
            val sameCol = others.any { colOf(it.from) == colOf(move.from) }
            val sameRow = others.any { rowOf(it.from) == rowOf(move.from) }
            if (!sameCol) {
              append(('a' + colOf(move.from)))
            } else if (!sameRow) {
              append(8 - rowOf(move.from))
            } else {
              append(move.fromSq)
            }
          }
          if (move.captured != null) append('x')
          append(move.toSq)
        }
      }
    }
    val oppInCheck = inCheck(turn)
    if (!oppInCheck) return base
    val oppHasMove = hasAnyLegalMove()
    return base + if (oppHasMove) "+" else "#"
  }

  private fun hasAnyLegalMove(): Boolean {
    val pseudo = generatePseudoMoves()
    val side = turn
    for (i in 0 until pseudo.size) {
      applyRawMove(pseudo[i])
      val safe = !inCheck(side)
      revertRawMove()
      if (safe) return true
    }
    return false
  }

  private fun generatePseudoMoves(): List<ChessMove> {
    val moves = ArrayList<ChessMove>(44)
    val us = turn
    val them = us.opposite()

    for (from in 0 until 64) {
      val piece = board[from] ?: continue
      if (piece.color != us) continue
      val r = rowOf(from)
      val c = colOf(from)

      when (piece.type) {
        PieceType.PAWN -> {
          val dir = if (us == PieceColor.WHITE) -1 else 1
          val startRow = if (us == PieceColor.WHITE) 6 else 1
          val promoRow = if (us == PieceColor.WHITE) 0 else 7
          val oneStepRow = r + dir
          if (oneStepRow in 0..7) {
            val oneIdx = oneStepRow * 8 + c
            if (board[oneIdx] == null) {
              if (oneStepRow == promoRow) {
                for (pt in PROMO_TYPES) {
                  moves.add(ChessMove(from, oneIdx, piece, promotion = pt))
                }
              } else {
                moves.add(ChessMove(from, oneIdx, piece))
                if (r == startRow) {
                  val twoIdx = (r + 2 * dir) * 8 + c
                  if (board[twoIdx] == null) {
                    moves.add(ChessMove(from, twoIdx, piece))
                  }
                }
              }
            }
            for (dc in intArrayOf(-1, 1)) {
              val nc = c + dc
              if (nc in 0..7) {
                val capIdx = oneStepRow * 8 + nc
                val target = board[capIdx]
                if (target != null && target.color == them) {
                  if (oneStepRow == promoRow) {
                    for (pt in PROMO_TYPES) {
                      moves.add(ChessMove(from, capIdx, piece, captured = target, promotion = pt))
                    }
                  } else {
                    moves.add(ChessMove(from, capIdx, piece, captured = target))
                  }
                } else if (capIdx == enPassantSquare) {
                  val epCaptured = board[r * 8 + nc]
                  if (epCaptured != null && epCaptured.color == them && epCaptured.type == PieceType.PAWN) {
                    moves.add(
                      ChessMove(
                        from = from,
                        to = capIdx,
                        piece = piece,
                        captured = epCaptured,
                        isEnPassant = true,
                      )
                    )
                  }
                }
              }
            }
          }
        }

        PieceType.KNIGHT -> {
          for (off in KNIGHT_OFFSETS) {
            val nr = r + off[0]
            val nc = c + off[1]
            if (nr in 0..7 && nc in 0..7) {
              val to = nr * 8 + nc
              val target = board[to]
              if (target == null || target.color == them) {
                moves.add(ChessMove(from, to, piece, captured = target))
              }
            }
          }
        }

        PieceType.BISHOP -> addSlidingMoves(from, piece, them, DIAG_DIRS, moves)
        PieceType.ROOK -> addSlidingMoves(from, piece, them, ORTHO_DIRS, moves)
        PieceType.QUEEN -> {
          addSlidingMoves(from, piece, them, ORTHO_DIRS, moves)
          addSlidingMoves(from, piece, them, DIAG_DIRS, moves)
        }

        PieceType.KING -> {
          for (dr in -1..1) {
            for (dc in -1..1) {
              if (dr == 0 && dc == 0) continue
              val nr = r + dr
              val nc = c + dc
              if (nr in 0..7 && nc in 0..7) {
                val to = nr * 8 + nc
                val target = board[to]
                if (target == null || target.color == them) {
                  moves.add(ChessMove(from, to, piece, captured = target))
                }
              }
            }
          }
          if (us == PieceColor.WHITE && from == 60 && !isSquareAttackedBy(60, them)) {
            if (castling.whiteKingside &&
              board[61] == null && board[62] == null &&
              board[63] == WHITE_ROOK &&
              !isSquareAttackedBy(61, them) && !isSquareAttackedBy(62, them)
            ) {
              moves.add(ChessMove(60, 62, piece, isKingsideCastle = true))
            }
            if (castling.whiteQueenside &&
              board[59] == null && board[58] == null && board[57] == null &&
              board[56] == WHITE_ROOK &&
              !isSquareAttackedBy(59, them) && !isSquareAttackedBy(58, them)
            ) {
              moves.add(ChessMove(60, 58, piece, isQueensideCastle = true))
            }
          } else if (us == PieceColor.BLACK && from == 4 && !isSquareAttackedBy(4, them)) {
            if (castling.blackKingside &&
              board[5] == null && board[6] == null &&
              board[7] == BLACK_ROOK &&
              !isSquareAttackedBy(5, them) && !isSquareAttackedBy(6, them)
            ) {
              moves.add(ChessMove(4, 6, piece, isKingsideCastle = true))
            }
            if (castling.blackQueenside &&
              board[3] == null && board[2] == null && board[1] == null &&
              board[0] == BLACK_ROOK &&
              !isSquareAttackedBy(3, them) && !isSquareAttackedBy(2, them)
            ) {
              moves.add(ChessMove(4, 2, piece, isQueensideCastle = true))
            }
          }
        }
      }
    }
    return moves
  }

  private fun addSlidingMoves(
    from: Int,
    piece: Piece,
    them: PieceColor,
    dirs: Array<IntArray>,
    out: MutableList<ChessMove>,
  ) {
    val r = rowOf(from)
    val c = colOf(from)
    for (dir in dirs) {
      var nr = r + dir[0]
      var nc = c + dir[1]
      while (nr in 0..7 && nc in 0..7) {
        val to = nr * 8 + nc
        val target = board[to]
        if (target == null) {
          out.add(ChessMove(from, to, piece))
        } else {
          if (target.color == them) {
            out.add(ChessMove(from, to, piece, captured = target))
          }
          break
        }
        nr += dir[0]
        nc += dir[1]
      }
    }
  }

  private fun applyRawMove(move: ChessMove) {
    val prevMask = castling.toMask()
    val prevHash = zobristHash
    undoStack.add(
      UndoState(
        castlingMask = prevMask,
        enPassantSquare = enPassantSquare,
        halfMoveClock = halfMoveClock,
        fullMoveNumber = fullMoveNumber,
        move = move,
        zobristHash = prevHash,
      )
    )

    // Remove old turn/castling/ep from hash
    var h = prevHash xor ZOBRIST_CASTLING[prevMask]
    enPassantSquare?.let { h = h xor ZOBRIST_EP[colOf(it)] }

    val fromPiece = move.piece
    val fromIdx = fromPiece.type.ordinal + (if (fromPiece.color == PieceColor.BLACK) 6 else 0)
    h = h xor ZOBRIST_PIECES[fromIdx * 64 + move.from]

    if (move.captured != null) {
      val capSq = if (move.isEnPassant) rowOf(move.from) * 8 + colOf(move.to) else move.to
      val capIdx = move.captured.type.ordinal + (if (move.captured.color == PieceColor.BLACK) 6 else 0)
      h = h xor ZOBRIST_PIECES[capIdx * 64 + capSq]
    }

    val placedPiece = if (move.promotion != null) Piece(move.promotion, move.piece.color) else move.piece
    val placedIdx = placedPiece.type.ordinal + (if (placedPiece.color == PieceColor.BLACK) 6 else 0)
    h = h xor ZOBRIST_PIECES[placedIdx * 64 + move.to]

    board[move.from] = null
    board[move.to] = placedPiece

    if (move.piece.type == PieceType.KING) {
      if (move.piece.color == PieceColor.WHITE) whiteKingSq = move.to else blackKingSq = move.to
    }

    if (move.isEnPassant) {
      val capRow = rowOf(move.from)
      val capCol = colOf(move.to)
      board[capRow * 8 + capCol] = null
    } else if (move.isKingsideCastle) {
      val r = rowOf(move.from)
      val rookFrom = r * 8 + 7
      val rookTo = r * 8 + 5
      val rook = board[rookFrom]
      board[rookFrom] = null
      board[rookTo] = rook
      if (rook != null) {
        val rIdx = PieceType.ROOK.ordinal + (if (rook.color == PieceColor.BLACK) 6 else 0)
        h = h xor ZOBRIST_PIECES[rIdx * 64 + rookFrom] xor ZOBRIST_PIECES[rIdx * 64 + rookTo]
      }
    } else if (move.isQueensideCastle) {
      val r = rowOf(move.from)
      val rookFrom = r * 8 + 0
      val rookTo = r * 8 + 3
      val rook = board[rookFrom]
      board[rookFrom] = null
      board[rookTo] = rook
      if (rook != null) {
        val rIdx = PieceType.ROOK.ordinal + (if (rook.color == PieceColor.BLACK) 6 else 0)
        h = h xor ZOBRIST_PIECES[rIdx * 64 + rookFrom] xor ZOBRIST_PIECES[rIdx * 64 + rookTo]
      }
    }

    var newMask = prevMask
    if (move.from == 60 || move.to == 60) newMask = newMask and 3.inv()
    if (move.from == 4 || move.to == 4) newMask = newMask and 12.inv()
    if (move.from == 63 || move.to == 63) newMask = newMask and 1.inv()
    if (move.from == 56 || move.to == 56) newMask = newMask and 2.inv()
    if (move.from == 7 || move.to == 7) newMask = newMask and 4.inv()
    if (move.from == 0 || move.to == 0) newMask = newMask and 8.inv()
    castling = CastlingRights.fromMask(newMask)
    h = h xor ZOBRIST_CASTLING[newMask]

    val nextEp = if (move.piece.type == PieceType.PAWN && abs(rowOf(move.to) - rowOf(move.from)) == 2) {
      ((rowOf(move.from) + rowOf(move.to)) / 2) * 8 + colOf(move.from)
    } else {
      null
    }
    enPassantSquare = nextEp
    nextEp?.let { h = h xor ZOBRIST_EP[colOf(it)] }

    halfMoveClock = if (move.piece.type == PieceType.PAWN || move.captured != null) 0 else halfMoveClock + 1
    if (turn == PieceColor.BLACK) fullMoveNumber++
    turn = turn.opposite()
    zobristHash = h xor ZOBRIST_SIDE
    cachedLegalMoves = null
  }

  private fun revertRawMove() {
    val prev = undoStack.removeAt(undoStack.lastIndex)
    val move = prev.move

    turn = turn.opposite()
    castling = CastlingRights.fromMask(prev.castlingMask)
    enPassantSquare = prev.enPassantSquare
    halfMoveClock = prev.halfMoveClock
    fullMoveNumber = prev.fullMoveNumber
    zobristHash = prev.zobristHash

    board[move.from] = move.piece
    board[move.to] = if (move.isEnPassant) null else move.captured

    if (move.piece.type == PieceType.KING) {
      if (move.piece.color == PieceColor.WHITE) whiteKingSq = move.from else blackKingSq = move.from
    }

    if (move.isEnPassant) {
      val capRow = rowOf(move.from)
      val capCol = colOf(move.to)
      board[capRow * 8 + capCol] = move.captured
    } else if (move.isKingsideCastle) {
      val r = rowOf(move.from)
      val rook = board[r * 8 + 5]
      board[r * 8 + 5] = null
      board[r * 8 + 7] = rook
    } else if (move.isQueensideCastle) {
      val r = rowOf(move.from)
      val rook = board[r * 8 + 3]
      board[r * 8 + 3] = null
      board[r * 8 + 0] = rook
    }

    cachedLegalMoves = null
  }

  fun makeMove(from: Int, to: Int, promotion: PieceType? = null): ChessMove? {
    val legal = legalMoves().find { m ->
      m.from == from && m.to == to && (m.promotion == null || m.promotion == (promotion ?: PieceType.QUEEN))
    } ?: return null
    applyRawMove(legal)
    positionCounts[zobristHash] = (positionCounts[zobristHash] ?: 0) + 1
    return legal
  }

  fun makeMove(fromSq: String, toSq: String, promotion: PieceType? = null): ChessMove? =
    makeMove(squareIndex(fromSq), squareIndex(toSq), promotion)

  fun makeMoveFast(move: ChessMove) {
    applyRawMove(move)
    positionCounts[zobristHash] = (positionCounts[zobristHash] ?: 0) + 1
  }

  fun undoMove(): ChessMove? {
    if (undoStack.isEmpty()) return null
    val count = positionCounts[zobristHash] ?: 0
    if (count <= 1) positionCounts.remove(zobristHash) else positionCounts[zobristHash] = count - 1
    val move = undoStack.last().move
    revertRawMove()
    return move
  }

  // Zero-HashMap-overhead search primitives for Alpha-Beta inner loop
  internal fun makeSearchMove(move: ChessMove) {
    applyRawMove(move)
  }

  internal fun undoSearchMove() {
    revertRawMove()
  }

  internal fun flipTurnCountOpponentCaptures(): Int {
    val prevTurn = turn
    val prevEp = enPassantSquare
    turn = turn.opposite()
    enPassantSquare = null
    val count = legalMovesFast().count { it.captured != null }
    turn = prevTurn
    enPassantSquare = prevEp
    return count
  }

  fun requiresPromotion(from: Int, to: Int): Boolean =
    legalMoves(from).any { it.to == to && it.promotion != null }

  fun isCheckmate(): Boolean = inCheck() && legalMovesFast().isEmpty()

  fun isStalemate(): Boolean = !inCheck() && legalMovesFast().isEmpty()

  fun isThreefoldRepetition(): Boolean = (positionCounts[zobristHash] ?: 0) >= 3

  fun isInsufficientMaterial(): Boolean {
    var whiteBishopCount = 0
    var blackBishopCount = 0
    var whiteBishopSq = -1
    var blackBishopSq = -1
    var minorCount = 0

    for (i in 0 until 64) {
      val p = board[i] ?: continue
      if (p.type == PieceType.KING) continue
      if (p.type == PieceType.PAWN || p.type == PieceType.ROOK || p.type == PieceType.QUEEN) return false
      minorCount++
      if (p.type == PieceType.BISHOP) {
        if (p.color == PieceColor.WHITE) {
          whiteBishopCount++
          whiteBishopSq = i
        } else {
          blackBishopCount++
          blackBishopSq = i
        }
      }
    }
    if (minorCount <= 1) return true
    if (minorCount == 2 && whiteBishopCount == 1 && blackBishopCount == 1) {
      val wSqColor = (rowOf(whiteBishopSq) + colOf(whiteBishopSq)) % 2
      val bSqColor = (rowOf(blackBishopSq) + colOf(blackBishopSq)) % 2
      return wSqColor == bSqColor
    }
    return false
  }

  fun isFiftyMoveDraw(): Boolean = halfMoveClock >= 100

  fun status(): GameStatus {
    val moves = legalMovesFast()
    if (moves.isEmpty()) {
      return if (inCheck()) GameStatus.Checkmate(turn.opposite()) else GameStatus.Stalemate
    }
    if (isThreefoldRepetition()) return GameStatus.RepetitionDraw
    if (isInsufficientMaterial()) return GameStatus.InsufficientMaterialDraw
    if (isFiftyMoveDraw()) return GameStatus.FiftyMoveDraw
    return GameStatus.Ongoing
  }

  fun soundKindFor(move: ChessMove): MoveSoundKind {
    return when {
      isCheckmate() -> MoveSoundKind.CHECKMATE
      inCheck() -> MoveSoundKind.CHECK
      move.promotion != null -> MoveSoundKind.PROMOTE
      move.isCastle -> MoveSoundKind.CASTLE
      move.captured != null -> MoveSoundKind.CAPTURE
      else -> MoveSoundKind.MOVE
    }
  }

  fun explainMove(move: ChessMove, beginnerMode: Boolean): String {
    val sb = StringBuilder()
    sb.append("${move.piece.type.displayName} ${move.fromSq}→${move.toSq}.")
    if (move.captured != null) {
      sb.append(" Captures a ${move.captured.type.displayName}.")
    }
    if (move.isCastle) {
      sb.append(" Castling: king to safety, rook into play.")
    }
    if (move.isEnPassant) {
      sb.append(" En passant capture.")
    }
    if (move.promotion != null) {
      sb.append(" Promoted to a ${move.promotion.displayName}.")
    }
    if (isCheckmate()) {
      sb.append(" Checkmate!")
    } else if (inCheck()) {
      sb.append(" Gives check.")
    }
    if (move.toSq[0] in "de" && move.toSq[1] in "45") {
      sb.append(" Fights for the centre.")
    }
    val homeRank = if (move.piece.color == PieceColor.WHITE) '1' else '8'
    if (move.piece.type in listOf(PieceType.KNIGHT, PieceType.BISHOP) && move.fromSq[1] == homeRank) {
      sb.append(" Develops a piece.")
    }
    if (!isCheckmate() && legalMovesFast().any { it.to == move.to && it.captured != null }) {
      sb.append(" ⚠ This piece can be captured next move.")
    }
    if (beginnerMode) {
      val s = move.san
      sb.append(" Notation “$s”: ")
      if (s.startsWith("O")) {
        sb.append("castling.")
      } else {
        if (s.isNotEmpty() && s[0] in "NBRQK") {
          val pName = move.piece.type.displayName.replaceFirstChar { it.uppercaseChar() }
          sb.append("$pName (capital letter = piece; pawns have no letter)")
        } else {
          sb.append("pawn move")
        }
        if ('x' in s) sb.append(", x = capture")
        if ('+' in s) sb.append(", + = check")
        if ('#' in s) sb.append(", # = checkmate")
        sb.append(".")
      }
    }
    return sb.toString()
  }

  companion object {
    const val STARTING_FEN = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1"
    private val WHITE_ROOK = Piece(PieceType.ROOK, PieceColor.WHITE)
    private val BLACK_ROOK = Piece(PieceType.ROOK, PieceColor.BLACK)
    private val PROMO_TYPES = listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
    private val KNIGHT_OFFSETS = arrayOf(
      intArrayOf(-2, -1), intArrayOf(-2, 1), intArrayOf(-1, -2), intArrayOf(-1, 2),
      intArrayOf(1, -2), intArrayOf(1, 2), intArrayOf(2, -1), intArrayOf(2, 1),
    )
    private val ORTHO_DIRS = arrayOf(
      intArrayOf(-1, 0), intArrayOf(1, 0), intArrayOf(0, -1), intArrayOf(0, 1),
    )
    private val DIAG_DIRS = arrayOf(
      intArrayOf(-1, -1), intArrayOf(-1, 1), intArrayOf(1, -1), intArrayOf(1, 1),
    )

    // Deterministic SplitMix64 Zobrist keys (zero runtime allocations)
    private val ZOBRIST_PIECES = LongArray(12 * 64)
    private val ZOBRIST_CASTLING = LongArray(16)
    private val ZOBRIST_EP = LongArray(8)
    private val ZOBRIST_SIDE: Long

    init {
      var seed = 0x9E3779B97F4A7C15UL.toLong()
      fun nextRand(): Long {
        seed += 0x9E3779B97F4A7C15UL.toLong()
        var z = seed
        z = (z xor (z ushr 30)) * -4658895280553007687L
        z = (z xor (z ushr 27)) * -7723592293110705685L
        return z xor (z ushr 31)
      }
      for (i in ZOBRIST_PIECES.indices) ZOBRIST_PIECES[i] = nextRand()
      for (i in ZOBRIST_CASTLING.indices) ZOBRIST_CASTLING[i] = nextRand()
      for (i in ZOBRIST_EP.indices) ZOBRIST_EP[i] = nextRand()
      ZOBRIST_SIDE = nextRand()
    }
  }
}
