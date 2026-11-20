package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType

private val WhiteFill = Color(0xFFFCFBF7)
private val WhiteStroke = Color(0xFF1E1D1B)
private val BlackFill = Color(0xFF242320)
private val BlackStroke = Color(0xFF121110)
private val BlackDetailStroke = Color(0xFFEBE8DF)

private object PieceGeometryCache {
  val strokeMain = Stroke(width = 1.6f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val strokeQueen = Stroke(width = 1.55f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val strokeThin = Stroke(width = 1.2f, cap = StrokeCap.Round, join = StrokeJoin.Round)
  val strokeTip = Stroke(width = 1.3f)

  val pawnBody = Path().apply {
    moveTo(22.5f, 9f)
    cubicTo(20.3f, 9f, 18.5f, 10.8f, 18.5f, 13f)
    cubicTo(18.5f, 14.4f, 19.2f, 15.6f, 20.3f, 16.3f)
    cubicTo(17.7f, 17.5f, 16f, 20f, 16f, 23f)
    cubicTo(16f, 24.8f, 16.8f, 26.4f, 18f, 27.5f)
    cubicTo(15f, 29f, 12.5f, 33f, 12.5f, 37.5f)
    lineTo(32.5f, 37.5f)
    cubicTo(32.5f, 33f, 30f, 29f, 27f, 27.5f)
    cubicTo(28.2f, 26.4f, 29f, 24.8f, 29f, 23f)
    cubicTo(29f, 20f, 27.3f, 17.5f, 24.7f, 16.3f)
    cubicTo(25.8f, 15.6f, 26.5f, 14.4f, 26.5f, 13f)
    cubicTo(26.5f, 10.8f, 24.7f, 9f, 22.5f, 9f)
    close()
  }
  val pawnBaseLine = Path().apply {
    moveTo(14.5f, 34.5f)
    lineTo(30.5f, 34.5f)
  }

  val rookBase = Path().apply {
    moveTo(9f, 39f); lineTo(36f, 39f); lineTo(36f, 36f); lineTo(9f, 36f); close()
  }
  val rookBody = Path().apply {
    moveTo(12f, 36f); lineTo(12f, 32f); lineTo(33f, 32f); lineTo(33f, 36f); close()
    moveTo(14f, 32f); lineTo(14f, 17f); lineTo(31f, 17f); lineTo(31f, 32f); close()
    moveTo(14f, 17f); lineTo(11f, 14f); lineTo(34f, 14f); lineTo(31f, 17f); close()
    moveTo(11f, 14f); lineTo(11f, 9f); lineTo(15f, 9f); lineTo(15f, 11.5f)
    lineTo(20f, 11.5f); lineTo(20f, 9f); lineTo(25f, 9f); lineTo(25f, 11.5f)
    lineTo(30f, 11.5f); lineTo(30f, 9f); lineTo(34f, 9f); lineTo(34f, 14f); close()
  }

  val bishopBase = Path().apply {
    moveTo(9f, 36f)
    cubicTo(12.4f, 35f, 19.1f, 36.4f, 22.5f, 34f)
    cubicTo(25.9f, 36.4f, 32.6f, 35f, 36f, 36f)
    cubicTo(36f, 36f, 37.6f, 36.5f, 39f, 38f)
    cubicTo(38.3f, 39f, 37.4f, 39f, 36f, 38.5f)
    cubicTo(32.6f, 37.5f, 25.9f, 38.9f, 22.5f, 37.5f)
    cubicTo(19.1f, 38.9f, 12.4f, 37.5f, 9f, 38.5f)
    cubicTo(7.6f, 39f, 6.7f, 39f, 6f, 38f)
    cubicTo(7.4f, 36.5f, 9f, 36f, 9f, 36f)
    close()
  }
  val bishopBody = Path().apply {
    moveTo(15f, 32f)
    cubicTo(17.5f, 34.5f, 27.5f, 34.5f, 30f, 32f)
    cubicTo(30.5f, 30.5f, 30f, 30f, 30f, 30f)
    cubicTo(30f, 27.5f, 27.5f, 26f, 27.5f, 26f)
    cubicTo(33f, 24.5f, 33.5f, 14.5f, 22.5f, 10.5f)
    cubicTo(11.5f, 14.5f, 12f, 24.5f, 17.5f, 26f)
    cubicTo(17.5f, 26f, 15f, 27.5f, 15f, 30f)
    cubicTo(15f, 30f, 14.5f, 30.5f, 15f, 32f)
    close()
  }

  val knightBody = Path().apply {
    moveTo(22f, 10f)
    cubicTo(32.5f, 11f, 38.5f, 18f, 38f, 39f)
    lineTo(15f, 39f)
    cubicTo(15f, 30f, 25f, 32.5f, 23f, 18f)
    close()
  }
  val knightHead = Path().apply {
    moveTo(24f, 18f)
    cubicTo(24.38f, 20.91f, 18.45f, 25.37f, 16f, 27f)
    cubicTo(13f, 29f, 13.18f, 31.34f, 11f, 31f)
    cubicTo(9.96f, 30.06f, 12.41f, 27.96f, 11f, 28f)
    cubicTo(10f, 28f, 11.19f, 29.23f, 10f, 30f)
    cubicTo(9f, 30f, 6f, 31f, 6f, 26f)
    cubicTo(6f, 24f, 12f, 14f, 12f, 14f)
    cubicTo(12f, 14f, 13.89f, 12.1f, 14f, 10.5f)
    cubicTo(13.27f, 9.51f, 13.5f, 8.5f, 13.5f, 7.5f)
    cubicTo(14.5f, 6.5f, 16.5f, 10f, 16.5f, 10f)
    lineTo(18.5f, 10f)
    cubicTo(18.5f, 10f, 19.28f, 8.01f, 21f, 7f)
    cubicTo(22f, 7f, 22f, 10f, 22f, 10f)
    close()
  }
  val knightMane = Path().apply {
    moveTo(24.5f, 11.5f)
    cubicTo(30.5f, 13.8f, 35.2f, 20.5f, 35f, 36.2f)
  }

  val queenCrown = Path().apply {
    moveTo(9f, 26f)
    cubicTo(17.5f, 24.5f, 30f, 24.5f, 36f, 26f)
    lineTo(38.5f, 13.5f); lineTo(31f, 25f); lineTo(30.7f, 10.9f)
    lineTo(25.5f, 24.5f); lineTo(22.5f, 10f); lineTo(19.5f, 24.5f)
    lineTo(14.3f, 10.9f); lineTo(14f, 25f); lineTo(6.5f, 13.5f); close()
  }
  val queenBase = Path().apply {
    moveTo(9f, 26f)
    cubicTo(9f, 28f, 10.5f, 28f, 11.5f, 30f)
    cubicTo(12.5f, 31.5f, 12.5f, 31f, 12f, 33.5f)
    cubicTo(10.5f, 34.5f, 10.5f, 36f, 10.5f, 36f)
    cubicTo(9f, 37.5f, 11f, 38.5f, 11f, 38.5f)
    cubicTo(17.5f, 39.5f, 27.5f, 39.5f, 34f, 38.5f)
    cubicTo(34f, 38.5f, 35.5f, 37.5f, 34f, 36f)
    cubicTo(34f, 36f, 34.5f, 34.5f, 33f, 33.5f)
    cubicTo(32.5f, 31f, 32.5f, 31.5f, 33.5f, 30f)
    cubicTo(34.5f, 28f, 36f, 28f, 36f, 26f)
    cubicTo(27.5f, 24.5f, 17.5f, 24.5f, 9f, 26f)
    close()
  }
  val queenTips = listOf(
    Offset(6f, 12f), Offset(14f, 9f), Offset(22.5f, 8f), Offset(31f, 9f), Offset(39f, 12f),
  )

  val kingCrownTop = Path().apply {
    moveTo(22.5f, 25f)
    cubicTo(22.5f, 25f, 27f, 17.5f, 25.5f, 14.5f)
    cubicTo(25.5f, 14.5f, 24.5f, 12f, 22.5f, 12f)
    cubicTo(20.5f, 12f, 19.5f, 14.5f, 19.5f, 14.5f)
    cubicTo(18f, 17.5f, 22.5f, 25f, 22.5f, 25f)
    close()
  }
  val kingBody = Path().apply {
    moveTo(11.5f, 37f)
    cubicTo(17f, 40.5f, 27f, 40.5f, 32.5f, 37f)
    lineTo(32.5f, 30f)
    cubicTo(32.5f, 30f, 41.5f, 25.5f, 38.5f, 19.5f)
    cubicTo(34.5f, 13f, 25f, 16f, 22.5f, 23.5f)
    lineTo(22.5f, 27f)
    lineTo(22.5f, 23.5f)
    cubicTo(19f, 16f, 9.5f, 13f, 6.5f, 19.5f)
    cubicTo(3.5f, 25.5f, 11.5f, 29.5f, 11.5f, 29.5f)
    close()
  }
  val kingBands = Path().apply {
    moveTo(11.5f, 30f); cubicTo(17f, 27f, 27f, 27f, 32.5f, 30f)
    moveTo(11.5f, 33.5f); cubicTo(17f, 30.5f, 27f, 30.5f, 32.5f, 33.5f)
    moveTo(11.5f, 37f); cubicTo(17f, 34f, 27f, 34f, 32.5f, 37f)
  }
}

fun buildPieceSpriteCache(sizePx: Int, density: Density): Map<Piece, ImageBitmap> {
  val dim = sizePx.coerceAtLeast(24)
  val map = HashMap<Piece, ImageBitmap>(12)
  val drawScope = CanvasDrawScope()
  val size = Size(dim.toFloat(), dim.toFloat())
  for (color in PieceColor.entries) {
    for (type in PieceType.entries) {
      val piece = Piece(type, color)
      val bmp = ImageBitmap(dim, dim, ImageBitmapConfig.Argb8888)
      val canvas = androidx.compose.ui.graphics.Canvas(bmp)
      drawScope.draw(density, LayoutDirection.Ltr, canvas, size) {
        drawChessPiece(piece = piece, alpha = 1f, lifted = false)
      }
      map[piece] = bmp
    }
  }
  return map
}

@Composable
fun ChessPieceView(
  piece: Piece,
  modifier: Modifier = Modifier,
  alpha: Float = 1f,
  lifted: Boolean = false,
) {
  val desc = "${piece.color.displayName()} ${piece.type.displayName}"
  Box(
    modifier = modifier.semantics { contentDescription = desc }
  ) {
    Canvas(modifier = Modifier.fillMaxSize()) {
      drawChessPiece(piece = piece, alpha = alpha, lifted = lifted)
    }
  }
}

fun DrawScope.drawChessPiece(
  piece: Piece,
  alpha: Float = 1f,
  lifted: Boolean = false,
) {
  val dim = size.minDimension
  if (dim <= 0f) return
  val scale = dim / 45f
  val dx = (size.width - dim) / 2f
  val dy = (size.height - dim) / 2f

  withTransform({
    translate(left = dx, top = dy)
    scale(scaleX = scale, scaleY = scale, pivot = Offset.Zero)
  }) {
    if (lifted) {
      drawOval(
        color = Color.Black.copy(alpha = 0.28f * alpha),
        topLeft = Offset(9f, 38.5f),
        size = Size(27f, 5f),
      )
    }
    val fill = (if (piece.color == PieceColor.WHITE) WhiteFill else BlackFill).copy(alpha = alpha)
    val outerStroke = (if (piece.color == PieceColor.WHITE) WhiteStroke else BlackStroke).copy(alpha = alpha)
    val detailStroke = (if (piece.color == PieceColor.WHITE) WhiteStroke else BlackDetailStroke).copy(alpha = alpha)
    val isBlack = piece.color == PieceColor.BLACK

    when (piece.type) {
      PieceType.PAWN -> {
        drawPath(PieceGeometryCache.pawnBody, color = fill)
        drawPath(PieceGeometryCache.pawnBody, color = outerStroke, style = PieceGeometryCache.strokeMain)
        drawPath(
          PieceGeometryCache.pawnBaseLine,
          color = if (isBlack) detailStroke.copy(alpha = detailStroke.alpha * 0.65f) else outerStroke,
          style = PieceGeometryCache.strokeThin,
        )
      }
      PieceType.ROOK -> {
        drawPath(PieceGeometryCache.rookBody, color = fill)
        drawPath(PieceGeometryCache.rookBase, color = fill)
        drawPath(PieceGeometryCache.rookBody, color = outerStroke, style = PieceGeometryCache.strokeMain)
        drawPath(PieceGeometryCache.rookBase, color = outerStroke, style = PieceGeometryCache.strokeMain)
        val lineCol = if (isBlack) detailStroke else outerStroke
        drawLine(lineCol, Offset(14.5f, 17f), Offset(30.5f, 17f), strokeWidth = 1.1f)
        drawLine(lineCol, Offset(14.5f, 32f), Offset(30.5f, 32f), strokeWidth = 1.1f)
        if (isBlack) {
          drawLine(detailStroke, Offset(12.5f, 35.8f), Offset(32.5f, 35.8f), strokeWidth = 1.1f)
        }
      }
      PieceType.BISHOP -> {
        drawPath(PieceGeometryCache.bishopBase, color = fill)
        drawPath(PieceGeometryCache.bishopBase, color = outerStroke, style = PieceGeometryCache.strokeMain)
        drawPath(PieceGeometryCache.bishopBody, color = fill)
        drawPath(PieceGeometryCache.bishopBody, color = outerStroke, style = PieceGeometryCache.strokeMain)
        drawCircle(color = fill, radius = 2.5f, center = Offset(22.5f, 8f))
        drawCircle(color = outerStroke, radius = 2.5f, center = Offset(22.5f, 8f), style = PieceGeometryCache.strokeMain)
        val lineCol = if (isBlack) detailStroke else outerStroke
        drawLine(lineCol, Offset(17.5f, 26f), Offset(27.5f, 26f), strokeWidth = 1.4f, cap = StrokeCap.Round)
        drawLine(lineCol, Offset(15.5f, 30f), Offset(29.5f, 30f), strokeWidth = 1.3f, cap = StrokeCap.Round)
        drawLine(lineCol, Offset(22.5f, 15.5f), Offset(22.5f, 20.5f), strokeWidth = 1.5f, cap = StrokeCap.Round)
        drawLine(lineCol, Offset(20f, 18f), Offset(25f, 18f), strokeWidth = 1.5f, cap = StrokeCap.Round)
      }
      PieceType.KNIGHT -> {
        drawPath(PieceGeometryCache.knightBody, color = fill)
        drawPath(PieceGeometryCache.knightHead, color = fill)
        drawPath(PieceGeometryCache.knightBody, color = outerStroke, style = PieceGeometryCache.strokeMain)
        drawPath(PieceGeometryCache.knightHead, color = outerStroke, style = PieceGeometryCache.strokeMain)
        val detailCol = if (isBlack) detailStroke else outerStroke
        drawCircle(color = detailCol, radius = 1.0f, center = Offset(15.2f, 15.5f))
        drawCircle(color = detailCol, radius = 0.85f, center = Offset(9.2f, 25.6f))
        if (isBlack) {
          drawPath(
            PieceGeometryCache.knightMane,
            color = detailStroke.copy(alpha = detailStroke.alpha * 0.7f),
            style = PieceGeometryCache.strokeThin,
          )
        }
      }
      PieceType.QUEEN -> {
        drawPath(PieceGeometryCache.queenCrown, color = fill)
        drawPath(PieceGeometryCache.queenBase, color = fill)
        drawPath(PieceGeometryCache.queenCrown, color = outerStroke, style = PieceGeometryCache.strokeQueen)
        drawPath(PieceGeometryCache.queenBase, color = outerStroke, style = PieceGeometryCache.strokeQueen)
        for (tip in PieceGeometryCache.queenTips) {
          drawCircle(color = fill, radius = 2.0f, center = tip)
          drawCircle(color = outerStroke, radius = 2.0f, center = tip, style = PieceGeometryCache.strokeTip)
        }
        val bandCol = if (isBlack) detailStroke else outerStroke
        drawLine(bandCol, Offset(11.5f, 30f), Offset(33.5f, 30f), strokeWidth = 1.2f, cap = StrokeCap.Round)
        drawLine(bandCol, Offset(12f, 33.5f), Offset(33f, 33.5f), strokeWidth = 1.2f, cap = StrokeCap.Round)
        if (isBlack) {
          drawLine(detailStroke, Offset(10.5f, 26.5f), Offset(34.5f, 26.5f), strokeWidth = 1.1f, cap = StrokeCap.Round)
        }
      }
      PieceType.KING -> {
        drawLine(outerStroke, Offset(22.5f, 11.63f), Offset(22.5f, 6f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        drawLine(outerStroke, Offset(20f, 8f), Offset(25f, 8f), strokeWidth = 1.8f, cap = StrokeCap.Round)
        drawPath(PieceGeometryCache.kingCrownTop, color = fill)
        drawPath(PieceGeometryCache.kingCrownTop, color = outerStroke, style = PieceGeometryCache.strokeMain)
        drawPath(PieceGeometryCache.kingBody, color = fill)
        drawPath(PieceGeometryCache.kingBody, color = outerStroke, style = PieceGeometryCache.strokeMain)
        val detailCol = if (isBlack) detailStroke else outerStroke
        drawPath(PieceGeometryCache.kingBands, color = detailCol, style = PieceGeometryCache.strokeThin)
      }
    }
  }
}
