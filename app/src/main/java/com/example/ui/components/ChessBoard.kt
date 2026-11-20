package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.colOf
import com.example.chess.rowOf
import com.example.ui.theme.DrawBlue
import com.example.ui.theme.DrawGreen
import com.example.ui.theme.DrawRed
import com.example.ui.theme.DrawYellow
import com.example.ui.theme.LocalRookieColors
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin

enum class DrawColor(val label: String, val color: Color) {
  GREEN("Green", DrawGreen),
  RED("Red", DrawRed),
  BLUE("Blue", DrawBlue),
  YELLOW("Yellow", DrawYellow),
}

data class BoardArrow(
  val from: Int,
  val to: Int,
  val color: DrawColor,
)

data class BoardDrawings(
  val highlights: Map<Int, DrawColor> = emptyMap(),
  val arrows: List<BoardArrow> = emptyList(),
)

data class MoveAnimationEvent(
  val id: Long,
  val from: Int,
  val to: Int,
  val piece: Piece,
  val capturedPiece: Piece? = null,
  val capturedSquare: Int? = null,
  val rookFrom: Int? = null,
  val rookTo: Int? = null,
  val rookPiece: Piece? = null,
)

data class PromotionPrompt(
  val from: Int,
  val to: Int,
  val color: PieceColor,
)

@Composable
fun ChessBoard(
  board: List<Piece?>,
  flipped: Boolean,
  selectedSquare: Int?,
  legalTargets: Set<Int>,
  lastMoveFrom: Int?,
  lastMoveTo: Int?,
  checkSquare: Int?,
  hintFrom: Int?,
  hintTo: Int?,
  drawMode: Boolean,
  drawColor: DrawColor,
  drawStartSquare: Int?,
  drawings: BoardDrawings,
  highlightsEnabled: Boolean,
  arrowsEnabled: Boolean,
  captureEffectsEnabled: Boolean,
  animationDurationMs: Int,
  animationEvent: MoveAnimationEvent?,
  shakeEvent: Pair<Long, Int>?,
  promotionPrompt: PromotionPrompt?,
  canInteractWithPiece: (Int) -> Boolean,
  onSquareTapped: (Int) -> Unit,
  onPieceDragDrop: (from: Int, to: Int) -> Unit,
  onDrawGesture: (from: Int, to: Int) -> Unit,
  onPromotionSelected: (PieceType) -> Unit,
  onPromotionDismissed: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val rookieColors = LocalRookieColors.current
  val density = LocalDensity.current
  val textMeasurer = rememberTextMeasurer()

  // Animation states — read ONLY inside Canvas draw lambdas so 0 recompositions happen per frame
  val moveProgress = remember { Animatable(1f) }
  var activeAnim by remember { mutableStateOf<MoveAnimationEvent?>(null) }
  LaunchedEffect(animationEvent?.id) {
    if (animationEvent != null && animationDurationMs > 0) {
      activeAnim = animationEvent
      moveProgress.snapTo(0f)
      moveProgress.animateTo(
        targetValue = 1f,
        animationSpec = tween(durationMillis = animationDurationMs, easing = FastOutSlowInEasing),
      )
      activeAnim = null
    } else {
      activeAnim = null
      moveProgress.snapTo(1f)
    }
  }

  val shakeOffset = remember { Animatable(0f) }
  var activeShakeSquare by remember { mutableStateOf<Int?>(null) }
  LaunchedEffect(shakeEvent?.first) {
    if (shakeEvent != null && animationDurationMs > 0) {
      activeShakeSquare = shakeEvent.second
      val offsets = listOf(-8f, 8f, -5f, 4f, 0f)
      for (dx in offsets) {
        shakeOffset.animateTo(dx, tween(durationMillis = 55))
      }
      activeShakeSquare = null
    } else {
      activeShakeSquare = null
      shakeOffset.snapTo(0f)
    }
  }

  val checkPulse = if (checkSquare != null && animationDurationMs > 0) {
    val infiniteTransition = rememberInfiniteTransition(label = "checkPulse")
    infiniteTransition.animateFloat(
      initialValue = 1.0f,
      targetValue = 1.09f,
      animationSpec = infiniteRepeatable(animation = tween(650), repeatMode = RepeatMode.Reverse),
      label = "checkScale",
    )
  } else {
    null
  }

  // Drag states — read inside Canvas draw lambda to prevent recomposition during 60/120Hz drags
  var dragFromSquare by remember { mutableStateOf<Int?>(null) }
  var dragPointerOffset by remember { mutableStateOf(Offset.Zero) }
  var dragPreviewArrowTo by remember { mutableStateOf<Int?>(null) }

  // Keep gesture lambdas up-to-date without cancelling pointerInput mid-drag
  val latestFlipped by rememberUpdatedState(flipped)
  val latestDrawMode by rememberUpdatedState(drawMode)
  val latestPromo by rememberUpdatedState(promotionPrompt)
  val latestSelected by rememberUpdatedState(selectedSquare)
  val latestCanInteract by rememberUpdatedState(canInteractWithPiece)
  val latestOnSquareTapped by rememberUpdatedState(onSquareTapped)
  val latestOnDragDrop by rememberUpdatedState(onPieceDragDrop)
  val latestOnDrawGesture by rememberUpdatedState(onDrawGesture)

  BoxWithConstraints(
    modifier = modifier
      .aspectRatio(1f)
      .clip(RoundedCornerShape(10.dp))
      .border(1.dp, rookieColors.subtleBorder, RoundedCornerShape(10.dp))
      .testTag("chess_board")
  ) {
    val boardPx = with(density) { maxWidth.toPx() }
    val sqPx = boardPx / 8f
    val sqPxInt = sqPx.roundToInt().coerceAtLeast(24)
    val sqDp = maxWidth / 8f

    // Pre-rendered ImageBitmap sprites at exact square pixel size
    val spriteCache = remember(sqPxInt, density) { buildPieceSpriteCache(sqPxInt, density) }

    // Pre-measured coordinate label layouts (zero Text composables in the board tree)
    val coordLabels = remember(flipped, textMeasurer) {
      val ranks = (0 until 8).map { vRow ->
        val bRow = if (flipped) 7 - vRow else vRow
        val bColLeft = if (flipped) 7 else 0
        val isDark = (bRow + bColLeft) % 2 == 1
        val color = if (isDark) rookieColors.lightSquare.copy(alpha = 0.85f) else rookieColors.darkSquare.copy(alpha = 0.85f)
        textMeasurer.measure(
          text = "${8 - bRow}",
          style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color),
        )
      }
      val files = (0 until 8).map { vCol ->
        val bRowBottom = if (flipped) 0 else 7
        val bCol = if (flipped) 7 - vCol else vCol
        val isDark = (bRowBottom + bCol) % 2 == 1
        val color = if (isDark) rookieColors.lightSquare.copy(alpha = 0.85f) else rookieColors.darkSquare.copy(alpha = 0.85f)
        textMeasurer.measure(
          text = "${'a' + bCol}",
          style = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color),
        )
      }
      ranks to files
    }

    fun squareAtOffset(offset: Offset): Int? {
      if (sqPx <= 0f) return null
      val vCol = (offset.x / sqPx).toInt()
      val vRow = (offset.y / sqPx).toInt()
      if (vCol !in 0..7 || vRow !in 0..7) return null
      val f = latestFlipped
      val bRow = if (f) 7 - vRow else vRow
      val bCol = if (f) 7 - vCol else vCol
      return bRow * 8 + bCol
    }

    fun visualPosOf(square: Int): Pair<Int, Int> {
      val bRow = rowOf(square)
      val bCol = colOf(square)
      val vRow = if (flipped) 7 - bRow else bRow
      val vCol = if (flipped) 7 - bCol else bCol
      return vCol to vRow
    }

    Box(
      modifier = Modifier
        .fillMaxSize()
        .pointerInput(sqPx) {
          detectTapGestures { offset ->
            if (latestPromo != null) return@detectTapGestures
            val sq = squareAtOffset(offset) ?: return@detectTapGestures
            latestOnSquareTapped(sq)
          }
        }
        .pointerInput(sqPx) {
          detectDragGestures(
            onDragStart = { startOffset ->
              if (latestPromo != null) return@detectDragGestures
              val sq = squareAtOffset(startOffset) ?: return@detectDragGestures
              if (latestDrawMode) {
                dragFromSquare = sq
                dragPreviewArrowTo = sq
              } else if (latestCanInteract(sq)) {
                dragFromSquare = sq
                dragPointerOffset = startOffset
                if (latestSelected != sq) {
                  latestOnSquareTapped(sq)
                }
              }
            },
            onDrag = { change, _ ->
              change.consume()
              val startSq = dragFromSquare ?: return@detectDragGestures
              if (latestDrawMode) {
                dragPreviewArrowTo = squareAtOffset(change.position) ?: startSq
              } else {
                dragPointerOffset = change.position
              }
            },
            onDragEnd = {
              val fromSq = dragFromSquare
              if (fromSq != null) {
                if (latestDrawMode) {
                  val toSq = dragPreviewArrowTo ?: fromSq
                  latestOnDrawGesture(fromSq, toSq)
                } else {
                  val toSq = squareAtOffset(dragPointerOffset)
                  if (toSq != null && toSq != fromSq) {
                    latestOnDragDrop(fromSq, toSq)
                  }
                }
              }
              dragFromSquare = null
              dragPreviewArrowTo = null
            },
            onDragCancel = {
              dragFromSquare = null
              dragPreviewArrowTo = null
            },
          )
        }
    ) {
      // Layer 1: Static squares, highlights, coordinate labels, and legal move dots
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = size.width / 8f
        for (vRow in 0 until 8) {
          for (vCol in 0 until 8) {
            val bRow = if (flipped) 7 - vRow else vRow
            val bCol = if (flipped) 7 - vCol else vCol
            val sq = bRow * 8 + bCol
            val isDark = (bRow + bCol) % 2 == 1
            val topLeft = Offset(vCol * cell, vRow * cell)
            val cellSize = Size(cell, cell)

            drawRect(
              color = if (isDark) rookieColors.darkSquare else rookieColors.lightSquare,
              topLeft = topLeft,
              size = cellSize,
            )

            if (highlightsEnabled && (sq == lastMoveFrom || sq == lastMoveTo)) {
              drawRect(color = rookieColors.lastMoveTint, topLeft = topLeft, size = cellSize)
            }
            if (highlightsEnabled && sq == selectedSquare) {
              drawRect(color = rookieColors.selectedTint, topLeft = topLeft, size = cellSize)
            }
            if (sq == hintFrom) {
              drawRect(color = rookieColors.hintOriginTint, topLeft = topLeft, size = cellSize)
            } else if (sq == hintTo) {
              drawRect(color = rookieColors.hintTargetTint, topLeft = topLeft, size = cellSize)
            }
            if (sq == checkSquare) {
              val center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f)
              drawRect(
                brush = Brush.radialGradient(
                  colors = listOf(rookieColors.checkGlow, rookieColors.checkGlow.copy(alpha = 0f)),
                  center = center,
                  radius = cell * 0.65f,
                ),
                topLeft = topLeft,
                size = cellSize,
              )
            }

            if (highlightsEnabled) {
              drawings.highlights[sq]?.let { dc ->
                drawRect(
                  color = dc.color.copy(alpha = 0.52f),
                  topLeft = topLeft,
                  size = cellSize,
                )
              }
            }

            if (drawMode && sq == drawStartSquare) {
              val center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f)
              drawCircle(
                color = drawColor.color,
                radius = cell * 0.42f,
                center = center,
                style = Stroke(
                  width = cell * 0.07f,
                  pathEffect = PathEffect.dashPathEffect(floatArrayOf(cell * 0.14f, cell * 0.1f)),
                ),
              )
            }

            if (sq in legalTargets) {
              val center = Offset(topLeft.x + cell / 2f, topLeft.y + cell / 2f)
              if (board[sq] != null) {
                drawCircle(
                  color = Color.Black.copy(alpha = 0.28f),
                  radius = cell * 0.42f,
                  center = center,
                  style = Stroke(width = cell * 0.11f),
                )
              } else {
                drawCircle(
                  color = Color.Black.copy(alpha = 0.25f),
                  radius = cell * 0.15f,
                  center = center,
                )
              }
            }
          }
        }

        // Rank & File labels drawn directly on Canvas
        val (ranks, files) = coordLabels
        val pad = 2.5.dp.toPx()
        for (vRow in 0 until 8) {
          drawText(
            textLayoutResult = ranks[vRow],
            topLeft = Offset(pad, vRow * cell + pad),
          )
        }
        for (vCol in 0 until 8) {
          val layout = files[vCol]
          drawText(
            textLayoutResult = layout,
            topLeft = Offset(
              (vCol + 1) * cell - layout.size.width - pad,
              8f * cell - layout.size.height - pad * 0.5f,
            ),
          )
        }
      }

      // Layer 2: Single-Canvas GPU Sprite Layer for all pieces, animations, dragging, and arrows
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cell = size.width / 8f
        val dstSize = IntSize(cell.roundToInt().coerceAtLeast(1), cell.roundToInt().coerceAtLeast(1))
        val anim = activeAnim
        val progress = moveProgress.value
        val shake = shakeOffset.value
        val pulse = checkPulse?.value ?: 1f
        val activeDragSq = if (!drawMode) dragFromSquare else null
        val pointer = dragPointerOffset

        // Captured ghost fade-out
        if (anim != null && captureEffectsEnabled && anim.capturedPiece != null && anim.capturedSquare != null) {
          val sprite = spriteCache[anim.capturedPiece]
          if (sprite != null) {
            val (cvCol, cvRow) = visualPosOf(anim.capturedSquare)
            val ghostAlpha = (1f - progress).coerceIn(0f, 1f)
            val ghostScale = 0.55f + 0.45f * ghostAlpha
            drawSprite(
              sprite = sprite,
              xPx = cvCol * cell,
              yPx = cvRow * cell,
              cell = cell,
              dstSize = dstSize,
              scale = ghostScale,
              alpha = ghostAlpha,
              lifted = false,
            )
          }
        }

        // Resting and animating pieces
        for (sq in 0 until 64) {
          val piece = board[sq] ?: continue
          if (activeDragSq == sq) continue
          val sprite = spriteCache[piece] ?: continue

          val isAnimatingMain = anim != null && anim.to == sq
          val isAnimatingRook = anim != null && anim.rookTo == sq && anim.rookFrom != null
          val (targetCol, targetRow) = visualPosOf(sq)

          val xPx: Float
          val yPx: Float
          val isLifted: Boolean
          if (isAnimatingMain && anim != null) {
            val (fromCol, fromRow) = visualPosOf(anim.from)
            xPx = (fromCol + (targetCol - fromCol) * progress) * cell
            yPx = (fromRow + (targetRow - fromRow) * progress) * cell
            isLifted = true
          } else if (isAnimatingRook && anim != null && anim.rookFrom != null) {
            val (fromCol, fromRow) = visualPosOf(anim.rookFrom)
            xPx = (fromCol + (targetCol - fromCol) * progress) * cell
            yPx = (fromRow + (targetRow - fromRow) * progress) * cell
            isLifted = true
          } else {
            xPx = targetCol * cell + if (activeShakeSquare == sq) shake * 2f else 0f
            yPx = targetRow * cell
            isLifted = selectedSquare == sq
          }

          val scale = when {
            sq == checkSquare -> pulse
            isLifted -> 1.09f
            else -> 1f
          }

          drawSprite(
            sprite = sprite,
            xPx = xPx,
            yPx = yPx,
            cell = cell,
            dstSize = dstSize,
            scale = scale,
            alpha = 1f,
            lifted = isLifted,
          )
        }

        // Dragged piece under finger
        if (activeDragSq != null) {
          val draggedPiece = board[activeDragSq]
          val sprite = draggedPiece?.let { spriteCache[it] }
          if (sprite != null) {
            drawSprite(
              sprite = sprite,
              xPx = pointer.x - cell / 2f,
              yPx = pointer.y - cell / 2f,
              cell = cell,
              dstSize = dstSize,
              scale = 1.2f,
              alpha = 1f,
              lifted = true,
            )
          }
        }

        // Arrows
        if (arrowsEnabled) {
          for (arrow in drawings.arrows) {
            drawChessArrow(arrow.from, arrow.to, arrow.color.color, flipped, cell, alpha = 0.86f)
          }
          val previewFrom = dragFromSquare
          val previewTo = dragPreviewArrowTo
          if (drawMode && previewFrom != null && previewTo != null && previewFrom != previewTo) {
            drawChessArrow(previewFrom, previewTo, drawColor.color, flipped, cell, alpha = 0.55f)
          }
        }
      }

      // Layer 3: Promotion Picker Overlay (only composed when active)
      if (promotionPrompt != null) {
        val (pCol, pRow) = visualPosOf(promotionPrompt.to)
        val anchorTop = pRow == 0
        Box(
          modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(onClick = onPromotionDismissed)
            .testTag("promotion_overlay")
        ) {
          Surface(
            shape = RoundedCornerShape(10.dp),
            tonalElevation = 8.dp,
            shadowElevation = 10.dp,
            color = Color(0xFFE9E6DF),
            modifier = Modifier
              .offset {
                val yOffset = if (anchorTop) 0 else (boardPx - sqPx * 4).roundToInt().coerceAtLeast(0)
                IntOffset((pCol * sqPx).roundToInt(), yOffset)
              }
              .size(width = sqDp, height = sqDp * 4),
          ) {
            val choices = if (anchorTop) {
              listOf(PieceType.QUEEN, PieceType.ROOK, PieceType.BISHOP, PieceType.KNIGHT)
            } else {
              listOf(PieceType.KNIGHT, PieceType.BISHOP, PieceType.ROOK, PieceType.QUEEN)
            }
            Column(
              modifier = Modifier.fillMaxSize(),
              verticalArrangement = Arrangement.SpaceEvenly,
              horizontalAlignment = Alignment.CenterHorizontally,
            ) {
              choices.forEach { pt ->
                Box(
                  contentAlignment = Alignment.Center,
                  modifier = Modifier
                    .size(sqDp)
                    .clickable { onPromotionSelected(pt) }
                    .semantics { contentDescription = "Promote to ${pt.displayName}" }
                    .testTag("promote_${pt.symbol}"),
                ) {
                  ChessPieceView(
                    piece = Piece(pt, promotionPrompt.color),
                    modifier = Modifier.fillMaxSize().padding(4.dp),
                  )
                }
              }
            }
          }
        }
      }
    }
  }
}

private fun DrawScope.drawSprite(
  sprite: ImageBitmap,
  xPx: Float,
  yPx: Float,
  cell: Float,
  dstSize: IntSize,
  scale: Float,
  alpha: Float,
  lifted: Boolean,
) {
  if (lifted) {
    drawOval(
      color = Color.Black.copy(alpha = 0.25f * alpha),
      topLeft = Offset(xPx + cell * 0.18f, yPx + cell * 0.84f),
      size = Size(cell * 0.64f, cell * 0.12f),
    )
  }
  if (scale == 1f) {
    drawImage(
      image = sprite,
      dstOffset = IntOffset(xPx.roundToInt(), yPx.roundToInt()),
      dstSize = dstSize,
      alpha = alpha,
    )
  } else {
    withTransform({
      translate(left = xPx, top = yPx)
      scale(scaleX = scale, scaleY = scale, pivot = Offset(cell * 0.5f, cell * 0.5f))
    }) {
      drawImage(
        image = sprite,
        dstOffset = IntOffset.Zero,
        dstSize = dstSize,
        alpha = alpha,
      )
    }
  }
}

private fun DrawScope.drawChessArrow(
  from: Int,
  to: Int,
  color: Color,
  flipped: Boolean,
  cell: Float,
  alpha: Float,
) {
  if (from == to) return
  val fRow = if (flipped) 7 - rowOf(from) else rowOf(from)
  val fCol = if (flipped) 7 - colOf(from) else colOf(from)
  val tRow = if (flipped) 7 - rowOf(to) else rowOf(to)
  val tCol = if (flipped) 7 - colOf(to) else colOf(to)

  val x1 = fCol * cell + cell * 0.5f
  val y1 = fRow * cell + cell * 0.5f
  val x2 = tCol * cell + cell * 0.5f
  val y2 = tRow * cell + cell * 0.5f

  val dx = tCol - fCol
  val dy = tRow - fRow
  val isKnight = abs(dx * dy) == 2

  val cx: Float
  val cy: Float
  if (isKnight) {
    if (abs(dx) > abs(dy)) {
      cx = x2; cy = y1
    } else {
      cx = x1; cy = y2
    }
  } else {
    cx = x1; cy = y1
  }

  val len = hypot(x2 - cx, y2 - cy)
  if (len <= 0.001f) return
  val ux = (x2 - cx) / len
  val uy = (y2 - cy) / len

  val headLength = cell * 0.34f
  val headWidth = cell * 0.23f
  val bx = x2 - ux * headLength
  val by = y2 - uy * headLength

  val lineColor = color.copy(alpha = alpha)
  val shaftPath = Path().apply {
    moveTo(x1, y1)
    if (isKnight) lineTo(cx, cy)
    lineTo(bx, by)
  }
  drawPath(
    path = shaftPath,
    color = lineColor,
    style = Stroke(width = cell * 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round),
  )

  val angle = atan2(y2 - cy, x2 - cx)
  val p1x = bx + headWidth * cos(angle + Math.PI / 2).toFloat()
  val p1y = by + headWidth * sin(angle + Math.PI / 2).toFloat()
  val p2x = bx + headWidth * cos(angle - Math.PI / 2).toFloat()
  val p2y = by + headWidth * sin(angle - Math.PI / 2).toFloat()

  val headPath = Path().apply {
    moveTo(x2, y2)
    lineTo(p1x, p1y)
    lineTo(p2x, p2y)
    close()
  }
  drawPath(path = headPath, color = lineColor)
}
