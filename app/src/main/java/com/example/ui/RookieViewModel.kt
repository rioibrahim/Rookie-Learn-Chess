package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.audio.SoundEngine
import com.example.chess.AI_LEVELS
import com.example.chess.ChessAi
import com.example.chess.ChessGame
import com.example.chess.ChessMove
import com.example.chess.GameStatus
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.chess.colOf
import com.example.chess.rowOf
import com.example.chess.squareIndex
import com.example.content.RookieCurriculum
import com.example.data.AnimationSpeed
import com.example.data.AppThemeMode
import com.example.data.ProgressRepository
import com.example.data.ProgressSnapshot
import com.example.data.UserSettings
import com.example.ui.components.BoardArrow
import com.example.ui.components.BoardDrawings
import com.example.ui.components.DrawColor
import com.example.ui.components.MoveAnimationEvent
import com.example.ui.components.PromotionPrompt
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.max
import kotlin.random.Random

enum class RookieTab(val label: String) {
  HOME("Dashboard"),
  LEARN("Learn"),
  PUZZLES("Puzzles"),
  PLAY("Play AI"),
  SANDBOX("Sandbox"),
}

data class RookieUiState(
  val activeTab: RookieTab = RookieTab.HOME,
  val board: List<Piece?> = ChessGame().boardSnapshot(),
  val turn: PieceColor = PieceColor.WHITE,
  val inCheckSquare: Int? = null,
  val fen: String = ChessGame.STARTING_FEN,
  val pgn: String = "",
  val moveHistory: List<ChessMove> = emptyList(),
  val selectedSquare: Int? = null,
  val legalTargets: Set<Int> = emptySet(),
  val lastMoveFrom: Int? = null,
  val lastMoveTo: Int? = null,
  val flipped: Boolean = false,
  val canUndo: Boolean = false,
  val canRedo: Boolean = false,

  // Feedback & hints
  val message: String = "",
  val isErrorMessage: Boolean = false,
  val hintLevel: Int = 0,
  val hintMessage: String = "",
  val hintFrom: Int? = null,
  val hintTo: Int? = null,
  val explanation: String = "",

  // Mode specifics
  val lessonIndex: Int = 0,
  val stepIndex: Int = 0,
  val stepSolved: Boolean = false,
  val puzzleIndex: Int = 0,
  val puzzleSolved: Boolean = false,
  val playerColor: PieceColor = PieceColor.WHITE,
  val aiLevelIndex: Int = 1,
  val isAiThinking: Boolean = false,
  val aiTelemetryText: String = "",
  val gameOverText: String = "",

  // Board interactions
  val drawMode: Boolean = false,
  val drawColor: DrawColor = DrawColor.GREEN,
  val drawStartSquare: Int? = null,
  val drawings: BoardDrawings = BoardDrawings(),
  val promotionPrompt: PromotionPrompt? = null,
  val animationEvent: MoveAnimationEvent? = null,
  val shakeEvent: Pair<Long, Int>? = null,

  // Persistence & settings
  val progress: ProgressSnapshot = ProgressSnapshot(),
  val settings: UserSettings = UserSettings(),
  val showSettingsSheet: Boolean = false,
) {
  val hasDrawings: Boolean get() = drawings.highlights.isNotEmpty() || drawings.arrows.isNotEmpty()
  val isPlayableSide: Boolean
    get() = when (activeTab) {
      RookieTab.HOME -> !stepSolved && gameOverText.isEmpty()
      RookieTab.LEARN -> !stepSolved
      RookieTab.PUZZLES -> !puzzleSolved
      RookieTab.PLAY -> !isAiThinking && gameOverText.isEmpty() && turn == playerColor
      RookieTab.SANDBOX -> true
    }
}

class RookieViewModel(
  private val repository: ProgressRepository,
  private val soundEngine: SoundEngine,
) : ViewModel() {

  private var game = ChessGame()
  private val redoStack = mutableListOf<ChessMove>()
  private var puzzleTriedThisAttempt = false
  private var aiJob: Job? = null
  private var animCounter = 0L
  private var shakeCounter = 0L
  private var animBusyUntilMs = 0L

  private val _uiState = MutableStateFlow(RookieUiState())
  val uiState: StateFlow<RookieUiState> = _uiState.asStateFlow()

  init {
    viewModelScope.launch {
      combine(repository.progressFlow, repository.settingsFlow) { progress, settings ->
        progress to settings
      }
        .distinctUntilChanged()
        .collect { (progress, settings) ->
          soundEngine.muted = settings.muted
          soundEngine.volume = settings.volume
          _uiState.update { state ->
            state.copy(progress = progress, settings = settings)
          }
        }
    }
    syncBoardToState()
  }

  override fun onCleared() {
    super.onCleared()
    cancelAi()
    soundEngine.release()
  }

  fun selectTab(tab: RookieTab) {
    if (_uiState.value.activeTab == tab) return
    soundEngine.playClick()
    cancelAi()
    when (tab) {
      RookieTab.HOME -> {
        loadPosition(ChessGame.STARTING_FEN, flipped = false)
        _uiState.update {
          it.copy(
            activeTab = tab,
            message = "",
            isErrorMessage = false,
            explanation = "",
          )
        }
      }
      RookieTab.LEARN -> {
        _uiState.update { it.copy(activeTab = tab) }
        startLessonStep(_uiState.value.lessonIndex, _uiState.value.stepIndex)
      }
      RookieTab.PUZZLES -> {
        _uiState.update { it.copy(activeTab = tab) }
        startPuzzle(_uiState.value.puzzleIndex)
      }
      RookieTab.PLAY -> {
        _uiState.update { it.copy(activeTab = tab) }
        startNewAiGame(PieceColor.WHITE, flip = false)
      }
      RookieTab.SANDBOX -> {
        loadPosition(ChessGame.STARTING_FEN, flipped = false)
        _uiState.update {
          it.copy(
            activeTab = tab,
            message = "",
            isErrorMessage = false,
            explanation = "",
          )
        }
      }
    }
  }

  fun onBackPressed(): Boolean {
    val state = _uiState.value
    if (state.promotionPrompt != null) {
      dismissPromotion()
      return true
    }
    if (state.showSettingsSheet) {
      setShowSettings(false)
      return true
    }
    if (state.activeTab != RookieTab.HOME) {
      selectTab(RookieTab.HOME)
      return true
    }
    return false
  }

  fun flipBoard() {
    soundEngine.playClick()
    _uiState.update { it.copy(flipped = !it.flipped) }
  }

  fun canInteractWithPiece(square: Int): Boolean {
    val state = _uiState.value
    if (state.drawMode || state.promotionPrompt != null) return false
    if (!state.isPlayableSide && state.activeTab != RookieTab.SANDBOX) return false
    val piece = game.pieceAt(square) ?: return false
    if (piece.color != game.turn) return false
    if (state.activeTab == RookieTab.PLAY && piece.color != state.playerColor) return false
    return true
  }

  fun onSquareTapped(square: Int) {
    val state = _uiState.value
    if (state.drawMode) {
      handleDrawTap(square)
      return
    }
    if (state.promotionPrompt != null || System.currentTimeMillis() < animBusyUntilMs) return
    if (!state.isPlayableSide && state.activeTab != RookieTab.SANDBOX) return

    val sel = state.selectedSquare
    if (sel != null && square in state.legalTargets) {
      if (game.requiresPromotion(sel, square)) {
        _uiState.update {
          it.copy(
            promotionPrompt = PromotionPrompt(sel, square, game.pieceAt(sel)!!.color),
            selectedSquare = null,
            legalTargets = emptySet(),
          )
        }
      } else {
        executePlayerMove(sel, square, null)
      }
      return
    }

    val piece = game.pieceAt(square)
    val allowedSide = state.activeTab != RookieTab.PLAY || piece?.color == state.playerColor
    if (piece != null && piece.color == game.turn && allowedSide) {
      val legal = game.legalMoves(square).map { it.to }.toSet()
      soundEngine.playClick()
      _uiState.update {
        it.copy(
          selectedSquare = square,
          legalTargets = legal,
          hintFrom = null,
          hintTo = null,
          drawings = BoardDrawings(),
        )
      }
    } else {
      if (sel != null && piece != null && state.activeTab != RookieTab.SANDBOX) {
        soundEngine.playError()
        _uiState.update {
          it.copy(
            message = "That piece can’t move there — look for the dots showing legal squares.",
            isErrorMessage = true,
            selectedSquare = null,
            legalTargets = emptySet(),
            shakeEvent = (++shakeCounter) to sel,
          )
        }
      } else {
        _uiState.update {
          it.copy(
            selectedSquare = null,
            legalTargets = emptySet(),
            drawings = BoardDrawings(),
          )
        }
      }
    }
  }

  fun onPieceDragDrop(from: Int, to: Int) {
    val state = _uiState.value
    if (state.drawMode || state.promotionPrompt != null || System.currentTimeMillis() < animBusyUntilMs) return
    if (!state.isPlayableSide && state.activeTab != RookieTab.SANDBOX) return

    val legal = game.legalMoves(from).map { it.to }.toSet()
    if (to in legal) {
      if (game.requiresPromotion(from, to)) {
        _uiState.update {
          it.copy(
            promotionPrompt = PromotionPrompt(from, to, game.pieceAt(from)!!.color),
            selectedSquare = null,
            legalTargets = emptySet(),
          )
        }
      } else {
        executePlayerMove(from, to, null)
      }
    } else if (game.pieceAt(to) != null && state.activeTab != RookieTab.SANDBOX) {
      soundEngine.playError()
      _uiState.update {
        it.copy(
          message = "That piece can’t move there — look for the dots showing legal squares.",
          isErrorMessage = true,
          selectedSquare = null,
          legalTargets = emptySet(),
          shakeEvent = (++shakeCounter) to from,
        )
      }
    }
  }

  fun onPromotionSelected(pieceType: PieceType) {
    val prompt = _uiState.value.promotionPrompt ?: return
    _uiState.update { it.copy(promotionPrompt = null) }
    executePlayerMove(prompt.from, prompt.to, pieceType)
  }

  fun dismissPromotion() {
    _uiState.update { it.copy(promotionPrompt = null) }
  }

  private fun executePlayerMove(from: Int, to: Int, promotion: PieceType?) {
    val prevHistorySize = game.history().size
    val move = game.makeMove(from, to, promotion) ?: return
    redoStack.clear()

    val state = _uiState.value
    val beginner = state.settings.beginnerMode
    val moveExplanation = game.explainMove(move, beginner)
    val soundKind = game.soundKindFor(move)

    when (state.activeTab) {
      RookieTab.LEARN -> {
        val lesson = RookieCurriculum.lessons[state.lessonIndex]
        val step = lesson.steps[state.stepIndex]
        if (move.fromSq == step.bestFrom && move.toSq in step.acceptedDestinations) {
          val anim = createAnimationEvent(move, reversed = false)
          val durationMs = state.settings.effectiveAnimationDurationMs.toLong()
          animBusyUntilMs = System.currentTimeMillis() + durationMs
          soundEngine.playMoveSound(soundKind, delayMs = (durationMs * 0.8).toLong())
          soundEngine.playSuccess()

          val currentKey = RookieCurriculum.lessonStepKey(state.lessonIndex, state.stepIndex)
          val nextCompleted = state.progress.completedStepKeys + currentKey
          val allStepsDone = state.stepIndex == lesson.steps.lastIndex &&
            lesson.steps.indices.all { idx ->
              RookieCurriculum.lessonStepKey(state.lessonIndex, idx) in nextCompleted
            }
          val msg = step.why + " ✓" + if (allStepsDone) " 🎉 Lesson complete!" else ""

          viewModelScope.launch {
            repository.markLessonStepCompleted(state.lessonIndex, state.stepIndex)
          }

          _uiState.update {
            it.copy(
              stepSolved = true,
              message = msg,
              isErrorMessage = false,
              explanation = moveExplanation,
              lastMoveFrom = move.from,
              lastMoveTo = move.to,
              selectedSquare = null,
              legalTargets = emptySet(),
              hintFrom = null,
              hintTo = null,
              animationEvent = anim,
              drawings = if (state.settings.clearDrawingsOnMove) BoardDrawings() else state.drawings,
              drawStartSquare = null,
            )
          }
          syncBoardToState()
        } else {
          game.undoMove()
          soundEngine.playError()
          val errMsg = if (move.fromSq != step.bestFrom) {
            "That’s a legal move, but not with the piece we’re practising. No problem — the position was reset, try again!"
          } else {
            "That’s legal, but it isn’t the move this exercise asks for. I put the piece back — read the task once more or press Hint."
          }
          _uiState.update {
            it.copy(
              message = errMsg,
              isErrorMessage = true,
              explanation = "",
              lastMoveFrom = null,
              lastMoveTo = null,
              selectedSquare = null,
              legalTargets = emptySet(),
              hintFrom = null,
              hintTo = null,
              shakeEvent = (++shakeCounter) to from,
            )
          }
          syncBoardToState()
        }
      }

      RookieTab.PUZZLES -> {
        val puzzle = RookieCurriculum.puzzles[state.puzzleIndex]
        val isCorrect = if (puzzle.solutionUci != null) {
          move.fromSq + move.toSq == puzzle.solutionUci
        } else {
          move.san.endsWith("#")
        }

        if (isCorrect) {
          val anim = createAnimationEvent(move, reversed = false)
          val durationMs = state.settings.effectiveAnimationDurationMs.toLong()
          animBusyUntilMs = System.currentTimeMillis() + durationMs
          soundEngine.playMoveSound(soundKind, delayMs = (durationMs * 0.8).toLong())
          soundEngine.playSuccess()

          val firstTry = !puzzleTriedThisAttempt
          puzzleTriedThisAttempt = true
          if (firstTry) {
            viewModelScope.launch {
              repository.recordPuzzleAttempt(puzzle.index, puzzle.category, success = true)
            }
          }
          val solvedText = (puzzle.why
            ?: "${move.san} is checkmate: the king is attacked and has no escape, capture or block.") + " ✓"

          _uiState.update {
            it.copy(
              puzzleSolved = true,
              message = solvedText,
              isErrorMessage = false,
              explanation = moveExplanation,
              lastMoveFrom = move.from,
              lastMoveTo = move.to,
              selectedSquare = null,
              legalTargets = emptySet(),
              hintFrom = null,
              hintTo = null,
              animationEvent = anim,
              drawings = if (state.settings.clearDrawingsOnMove) BoardDrawings() else state.drawings,
              drawStartSquare = null,
            )
          }
          syncBoardToState()
        } else {
          game.undoMove()
          soundEngine.playError()
          val firstTry = !puzzleTriedThisAttempt
          puzzleTriedThisAttempt = true
          if (firstTry) {
            viewModelScope.launch {
              repository.recordPuzzleAttempt(puzzle.index, puzzle.category, success = false)
            }
          }
          _uiState.update {
            it.copy(
              message = "${move.san} isn’t it. Position reset — try another idea (hints are unlimited).",
              isErrorMessage = true,
              explanation = "",
              lastMoveFrom = null,
              lastMoveTo = null,
              selectedSquare = null,
              legalTargets = emptySet(),
              hintFrom = null,
              hintTo = null,
              shakeEvent = (++shakeCounter) to from,
            )
          }
          syncBoardToState()
        }
      }

      RookieTab.PLAY -> {
        val anim = createAnimationEvent(move, reversed = false)
        val durationMs = state.settings.effectiveAnimationDurationMs.toLong()
        animBusyUntilMs = System.currentTimeMillis() + durationMs
        soundEngine.playMoveSound(soundKind, delayMs = (durationMs * 0.8).toLong())

        _uiState.update {
          it.copy(
            explanation = moveExplanation,
            lastMoveFrom = move.from,
            lastMoveTo = move.to,
            selectedSquare = null,
            legalTargets = emptySet(),
            hintFrom = null,
            hintTo = null,
            hintMessage = "",
            hintLevel = 0,
            animationEvent = anim,
            drawings = if (state.settings.clearDrawingsOnMove) BoardDrawings() else state.drawings,
            drawStartSquare = null,
          )
        }
        syncBoardToState()
        if (!checkAndHandleGameEnd()) {
          triggerAiMove()
        }
      }

      RookieTab.HOME, RookieTab.SANDBOX -> {
        if (game.history().size > prevHistorySize) {
          val anim = createAnimationEvent(move, reversed = false)
          val durationMs = state.settings.effectiveAnimationDurationMs.toLong()
          animBusyUntilMs = System.currentTimeMillis() + durationMs
          soundEngine.playMoveSound(soundKind, delayMs = (durationMs * 0.8).toLong())
          _uiState.update {
            it.copy(
              explanation = moveExplanation,
              lastMoveFrom = move.from,
              lastMoveTo = move.to,
              selectedSquare = null,
              legalTargets = emptySet(),
              hintFrom = null,
              hintTo = null,
              animationEvent = anim,
              drawings = if (state.settings.clearDrawingsOnMove) BoardDrawings() else state.drawings,
              drawStartSquare = null,
            )
          }
          syncBoardToState()
        }
      }
    }
  }

  private fun checkAndHandleGameEnd(): Boolean {
    val status = game.status()
    if (!status.isOver) return false
    val playerCol = _uiState.value.playerColor
    viewModelScope.launch {
      when (status) {
        is GameStatus.Checkmate -> {
          if (status.winner == playerCol) {
            repository.recordGameOutcome(win = true)
          } else {
            repository.recordGameOutcome(loss = true)
          }
        }
        else -> repository.recordGameOutcome(draw = true)
      }
    }
    _uiState.update { it.copy(gameOverText = status.description(), isAiThinking = false) }
    return true
  }

  private fun triggerAiMove() {
    cancelAi()
    val level = AI_LEVELS[_uiState.value.aiLevelIndex]
    val fenSnapshot = game.fen()
    val thinkMs = ChessAi.thinkMillis(game, level)
    val startTimeMs = System.currentTimeMillis()

    _uiState.update {
      it.copy(
        isAiThinking = true,
        aiTelemetryText = "AI is thinking · Built-in search · depth ${level.depth}",
      )
    }

    aiJob = viewModelScope.launch {
      val move = withContext(Dispatchers.Default) {
        ChessAi.pickMove(
          sourceGame = game,
          level = level,
          budgetMillis = max(180L, (thinkMs * 0.7).toLong()),
        ) { telemetry ->
          val kNodes = telemetry.nodes / 1000
          val nodeStr = if (kNodes > 0) " · ${kNodes}k nodes" else " · ${telemetry.nodes} nodes"
          _uiState.update {
            it.copy(aiTelemetryText = "AI is thinking · Built-in search · depth ${telemetry.depth}$nodeStr")
          }
        }
      }
      if (move == null || game.fen() != fenSnapshot) {
        _uiState.update { it.copy(isAiThinking = false, aiTelemetryText = "") }
        return@launch
      }

      _uiState.update { it.copy(aiTelemetryText = "Move found — playing it now") }
      val elapsed = System.currentTimeMillis() - startTimeMs
      val animWait = animBusyUntilMs - System.currentTimeMillis() + 30L
      val remainingWait = max(0L, max(thinkMs - elapsed, animWait))
      if (remainingWait > 0L) delay(remainingWait)

      if (game.fen() != fenSnapshot) return@launch
      val played = game.makeMove(move.from, move.to, move.promotion) ?: run {
        _uiState.update { it.copy(isAiThinking = false, aiTelemetryText = "") }
        return@launch
      }

      val state = _uiState.value
      val anim = createAnimationEvent(played, reversed = false)
      val durationMs = state.settings.effectiveAnimationDurationMs.toLong()
      animBusyUntilMs = System.currentTimeMillis() + durationMs
      soundEngine.playMoveSound(game.soundKindFor(played), delayMs = (durationMs * 0.8).toLong())

      _uiState.update {
        it.copy(
          isAiThinking = false,
          aiTelemetryText = "",
          explanation = "Opponent: " + game.explainMove(played, state.settings.beginnerMode),
          lastMoveFrom = played.from,
          lastMoveTo = played.to,
          animationEvent = anim,
          drawings = if (state.settings.clearDrawingsOnMove) BoardDrawings() else state.drawings,
          drawStartSquare = null,
        )
      }
      syncBoardToState()
      checkAndHandleGameEnd()
    }
  }

  private fun cancelAi() {
    aiJob?.cancel()
    aiJob = null
    _uiState.update { it.copy(isAiThinking = false, aiTelemetryText = "") }
  }

  fun undo() {
    val state = _uiState.value
    if (state.isAiThinking) cancelAi()
    var count = if (state.activeTab == RookieTab.PLAY) {
      if (game.turn == state.playerColor) 2 else 1
    } else {
      1
    }
    var lastUndone: ChessMove? = null
    while (count-- > 0 && game.history().isNotEmpty()) {
      val m = game.undoMove()
      if (m != null) {
        redoStack.add(m)
        lastUndone = m
      }
    }
    if (lastUndone != null) {
      soundEngine.playClick()
      val prevMove = game.lastMove()
      val anim = createAnimationEvent(lastUndone, reversed = true)
      _uiState.update {
        it.copy(
          selectedSquare = null,
          legalTargets = emptySet(),
          hintFrom = null,
          hintTo = null,
          hintLevel = 0,
          hintMessage = "",
          stepSolved = false,
          puzzleSolved = false,
          gameOverText = "",
          lastMoveFrom = prevMove?.from,
          lastMoveTo = prevMove?.to,
          promotionPrompt = null,
          animationEvent = anim,
          drawings = if (state.settings.clearDrawingsOnMove) BoardDrawings() else state.drawings,
        )
      }
      syncBoardToState()
    }
  }

  fun redo() {
    if (redoStack.isEmpty()) return
    val next = redoStack.removeAt(redoStack.lastIndex)
    val played = game.makeMove(next.from, next.to, next.promotion) ?: return
    val state = _uiState.value
    val durationMs = state.settings.effectiveAnimationDurationMs.toLong()
    soundEngine.playMoveSound(game.soundKindFor(played), delayMs = (durationMs * 0.8).toLong())
    val anim = createAnimationEvent(played, reversed = false)
    _uiState.update {
      it.copy(
        lastMoveFrom = played.from,
        lastMoveTo = played.to,
        selectedSquare = null,
        legalTargets = emptySet(),
        stepSolved = state.activeTab == RookieTab.LEARN,
        puzzleSolved = state.activeTab == RookieTab.PUZZLES,
        animationEvent = anim,
        drawings = if (state.settings.clearDrawingsOnMove) BoardDrawings() else state.drawings,
      )
    }
    syncBoardToState()
  }

  fun showHint() {
    soundEngine.playClick()
    val state = _uiState.value
    when (state.activeTab) {
      RookieTab.PLAY -> {
        val gameCopy = game.copy()
        viewModelScope.launch {
          val suggested = withContext(Dispatchers.Default) {
            ChessAi.pickHintMove(gameCopy)
          } ?: return@launch
          _uiState.update {
            it.copy(
              hintLevel = 3,
              hintFrom = suggested.from,
              hintTo = suggested.to,
              hintMessage = "Suggested: ${suggested.san} (${suggested.piece.type.displayName} to ${suggested.toSq}), found by a 2-move-deep search.",
            )
          }
        }
      }
      RookieTab.LEARN -> {
        val step = RookieCurriculum.lessons[state.lessonIndex].steps[state.stepIndex]
        val nextLevel = (state.hintLevel + 1).coerceAtMost(4)
        val fromIdx = squareIndex(step.bestFrom)
        val toIdx = squareIndex(step.bestTo)
        val msg = when (nextLevel) {
          1 -> "💡 ${step.idea}"
          2 -> "Look at the highlighted piece — it’s the one to move."
          3 -> "The highlighted squares show the move."
          else -> "Full explanation: ${step.why}"
        }
        _uiState.update {
          it.copy(
            hintLevel = nextLevel,
            hintMessage = msg,
            hintFrom = if (nextLevel >= 2) fromIdx else null,
            hintTo = if (nextLevel >= 3) toIdx else null,
          )
        }
      }
      RookieTab.PUZZLES -> {
        val puzzle = RookieCurriculum.puzzles[state.puzzleIndex]
        val bestFromSq: String
        val bestToSq: String
        val idea: String
        val why: String
        if (puzzle.solutionUci != null) {
          bestFromSq = puzzle.solutionUci.substring(0, 2)
          bestToSq = puzzle.solutionUci.substring(2, 4)
          idea = "Look for a double attack on two valuable pieces."
          why = puzzle.why ?: ""
        } else {
          val mate = puzzle.mateMove() ?: return
          bestFromSq = mate.fromSq
          bestToSq = mate.toSq
          idea = "Start with checks. Which check leaves the king no flight squares?"
          why = "${mate.san} is checkmate."
        }
        val nextLevel = (state.hintLevel + 1).coerceAtMost(4)
        val msg = when (nextLevel) {
          1 -> "💡 $idea"
          2 -> "Look at the highlighted piece — it’s the one to move."
          3 -> "The highlighted squares show the move."
          else -> "Full explanation: $why"
        }
        _uiState.update {
          it.copy(
            hintLevel = nextLevel,
            hintMessage = msg,
            hintFrom = if (nextLevel >= 2) squareIndex(bestFromSq) else null,
            hintTo = if (nextLevel >= 3) squareIndex(bestToSq) else null,
          )
        }
      }
      else -> Unit
    }
  }

  fun restartCurrent() {
    soundEngine.playClick()
    val state = _uiState.value
    when (state.activeTab) {
      RookieTab.LEARN -> startLessonStep(state.lessonIndex, state.stepIndex)
      RookieTab.PUZZLES -> startPuzzle(state.puzzleIndex)
      RookieTab.SANDBOX -> {
        loadPosition(ChessGame.STARTING_FEN, flipped = state.flipped)
        _uiState.update { it.copy(explanation = "", message = "", isErrorMessage = false) }
      }
      else -> Unit
    }
  }

  fun selectLesson(lessonIndex: Int) {
    soundEngine.playClick()
    startLessonStep(lessonIndex, 0)
  }

  fun nextLessonStep() {
    soundEngine.playClick()
    val state = _uiState.value
    val lesson = RookieCurriculum.lessons[state.lessonIndex]
    if (state.stepIndex < lesson.steps.lastIndex) {
      startLessonStep(state.lessonIndex, state.stepIndex + 1)
    } else {
      val nextLesson = (state.lessonIndex + 1) % RookieCurriculum.lessons.size
      startLessonStep(nextLesson, 0)
    }
  }

  private fun startLessonStep(lessonIdx: Int, stepIdx: Int) {
    val lesson = RookieCurriculum.lessons[lessonIdx]
    val step = lesson.steps[stepIdx]
    loadPosition(step.fen, flipped = false)
    _uiState.update {
      it.copy(
        lessonIndex = lessonIdx,
        stepIndex = stepIdx,
        stepSolved = false,
        message = step.prompt,
        isErrorMessage = false,
        explanation = "",
      )
    }
  }

  fun nextPuzzle() {
    soundEngine.playClick()
    val next = (_uiState.value.puzzleIndex + 1) % RookieCurriculum.puzzles.size
    startPuzzle(next)
  }

  fun previousPuzzle() {
    soundEngine.playClick()
    val prev = (_uiState.value.puzzleIndex - 1).coerceAtLeast(0)
    startPuzzle(prev)
  }

  private fun startPuzzle(index: Int) {
    val puzzle = RookieCurriculum.puzzles[index]
    val puzzleGame = ChessGame(puzzle.fen)
    loadPosition(puzzle.fen, flipped = puzzleGame.turn == PieceColor.BLACK)
    puzzleTriedThisAttempt = false
    _uiState.update {
      it.copy(
        puzzleIndex = index,
        puzzleSolved = false,
        message = puzzle.prompt,
        isErrorMessage = false,
        explanation = "",
      )
    }
  }

  fun selectAiLevel(index: Int) {
    soundEngine.playClick()
    _uiState.update { it.copy(aiLevelIndex = index) }
  }

  fun startNewAiGame(sideChoice: PieceColor?, flip: Boolean? = null) {
    soundEngine.playClick()
    cancelAi()
    val chosen = sideChoice ?: if (Random.nextBoolean()) PieceColor.WHITE else PieceColor.BLACK
    val shouldFlip = flip ?: (chosen == PieceColor.BLACK)
    loadPosition(ChessGame.STARTING_FEN, flipped = shouldFlip)
    _uiState.update {
      it.copy(
        playerColor = chosen,
        gameOverText = "",
        explanation = "",
        message = "",
        isErrorMessage = false,
      )
    }
    if (chosen == PieceColor.BLACK) {
      triggerAiMove()
    }
  }

  fun resignAiGame() {
    val state = _uiState.value
    if (state.gameOverText.isNotEmpty() || game.history().isEmpty()) return
    soundEngine.playClick()
    cancelAi()
    viewModelScope.launch {
      repository.recordGameOutcome(loss = true)
    }
    _uiState.update { it.copy(gameOverText = "You resigned") }
  }

  // Drawing mode
  fun toggleDrawMode() {
    soundEngine.playClick()
    _uiState.update { it.copy(drawMode = !it.drawMode, drawStartSquare = null) }
  }

  fun selectDrawColor(color: DrawColor) {
    soundEngine.playClick()
    _uiState.update { it.copy(drawColor = color, drawStartSquare = null) }
  }

  fun clearDrawings() {
    soundEngine.playClick()
    _uiState.update { it.copy(drawings = BoardDrawings(), drawStartSquare = null) }
  }

  private fun handleDrawTap(square: Int) {
    val start = _uiState.value.drawStartSquare
    if (start == null) {
      soundEngine.playClick()
      _uiState.update { it.copy(drawStartSquare = square) }
    } else {
      onDrawGesture(start, square)
      _uiState.update { it.copy(drawStartSquare = null) }
    }
  }

  fun onDrawGesture(from: Int, to: Int) {
    val state = _uiState.value
    val color = state.drawColor
    val current = state.drawings
    if (from == to) {
      if (!state.settings.highlightsEnabled) return
      val nextHighlights = current.highlights.toMutableMap()
      if (nextHighlights[from] == color) {
        nextHighlights.remove(from)
      } else {
        nextHighlights[from] = color
      }
      soundEngine.playClick()
      _uiState.update {
        it.copy(drawings = current.copy(highlights = nextHighlights), drawStartSquare = null)
      }
    } else {
      if (!state.settings.arrowsEnabled) return
      val nextArrows = current.arrows.toMutableList()
      val existingIdx = nextArrows.indexOfFirst { it.from == from && it.to == to }
      if (existingIdx >= 0) {
        val sameColor = nextArrows[existingIdx].color == color
        nextArrows.removeAt(existingIdx)
        if (!sameColor) nextArrows.add(BoardArrow(from, to, color))
      } else {
        nextArrows.add(BoardArrow(from, to, color))
      }
      soundEngine.playClick()
      _uiState.update {
        it.copy(drawings = current.copy(arrows = nextArrows), drawStartSquare = null)
      }
    }
  }

  // Settings
  fun setShowSettings(show: Boolean) {
    soundEngine.playClick()
    _uiState.update { it.copy(showSettingsSheet = show) }
  }

  fun toggleMuted() {
    viewModelScope.launch {
      repository.updateSettings { it.copy(muted = !it.muted) }
    }
  }

  fun setVolume(volume: Float) {
    soundEngine.playVolumePreview(volume)
    viewModelScope.launch {
      repository.updateSettings { it.copy(volume = volume) }
    }
  }

  fun toggleBeginnerMode() {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(beginnerMode = !it.beginnerMode) }
    }
  }

  fun toggleAnimations() {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(animationsEnabled = !it.animationsEnabled) }
    }
  }

  fun selectAnimationSpeed(speed: AnimationSpeed) {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(animationSpeed = speed) }
    }
  }

  fun toggleCaptureEffects() {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(captureEffects = !it.captureEffects) }
    }
  }

  fun toggleHighlights() {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(highlightsEnabled = !it.highlightsEnabled) }
    }
  }

  fun toggleArrows() {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(arrowsEnabled = !it.arrowsEnabled) }
    }
  }

  fun toggleClearOnMove() {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(clearDrawingsOnMove = !it.clearDrawingsOnMove) }
    }
  }

  fun toggleReducedMotion() {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(reducedMotion = !it.reducedMotion) }
    }
  }

  fun selectThemeMode(mode: AppThemeMode) {
    soundEngine.playClick()
    viewModelScope.launch {
      repository.updateSettings { it.copy(themeMode = mode) }
    }
  }

  private fun loadPosition(fen: String, flipped: Boolean) {
    cancelAi()
    animBusyUntilMs = 0L
    game = ChessGame(fen)
    redoStack.clear()
    _uiState.update {
      it.copy(
        flipped = flipped,
        selectedSquare = null,
        legalTargets = emptySet(),
        lastMoveFrom = null,
        lastMoveTo = null,
        hintLevel = 0,
        hintMessage = "",
        hintFrom = null,
        hintTo = null,
        drawings = BoardDrawings(),
        drawStartSquare = null,
        promotionPrompt = null,
        animationEvent = null,
        gameOverText = "",
      )
    }
    syncBoardToState()
  }

  private fun syncBoardToState() {
    val checkSq = if (game.inCheck()) game.kingSquare(game.turn).takeIf { it >= 0 } else null
    _uiState.update {
      it.copy(
        board = game.boardSnapshot(),
        turn = game.turn,
        inCheckSquare = checkSq,
        fen = game.fen(),
        pgn = game.pgn(),
        moveHistory = game.history(),
        canUndo = game.history().isNotEmpty(),
        canRedo = redoStack.isNotEmpty(),
      )
    }
  }

  private fun createAnimationEvent(move: ChessMove, reversed: Boolean): MoveAnimationEvent {
    val from = if (reversed) move.to else move.from
    val to = if (reversed) move.from else move.to
    val capSq = if (move.isEnPassant) rowOf(move.from) * 8 + colOf(move.to) else move.to

    var rookFrom: Int? = null
    var rookTo: Int? = null
    var rookPiece: Piece? = null
    if (move.isCastle) {
      val r = rowOf(move.from)
      val rf = r * 8 + if (move.isKingsideCastle) 7 else 0
      val rt = r * 8 + if (move.isKingsideCastle) 5 else 3
      rookFrom = if (reversed) rt else rf
      rookTo = if (reversed) rf else rt
      rookPiece = game.pieceAt(rookTo)
    }

    return MoveAnimationEvent(
      id = ++animCounter,
      from = from,
      to = to,
      piece = move.piece,
      capturedPiece = if (!reversed) move.captured else null,
      capturedSquare = if (!reversed && move.captured != null) capSq else null,
      rookFrom = rookFrom,
      rookTo = rookTo,
      rookPiece = rookPiece,
    )
  }

  class Factory(
    private val repository: ProgressRepository,
    private val soundEngine: SoundEngine,
  ) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return RookieViewModel(repository, soundEngine) as T
    }
  }
}
