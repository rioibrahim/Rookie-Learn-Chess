package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeOff
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.outlined.Dashboard
import androidx.compose.material.icons.outlined.Extension
import androidx.compose.material.icons.outlined.GridOn
import androidx.compose.material.icons.outlined.School
import androidx.compose.material.icons.outlined.SmartToy
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Extension
import androidx.compose.material.icons.rounded.GridOn
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SmartToy
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.chess.Piece
import com.example.chess.PieceColor
import com.example.chess.PieceType
import com.example.data.AppThemeMode
import com.example.ui.components.BoardControlsRow
import com.example.ui.components.ChessBoard
import com.example.ui.components.ChessPieceView
import com.example.ui.components.SettingsSheet
import com.example.ui.screens.DashboardPanel
import com.example.ui.screens.LearnPanel
import com.example.ui.screens.PlayAiPanel
import com.example.ui.screens.PuzzlesPanel
import com.example.ui.screens.SandboxPanel
import com.example.ui.theme.RookieTheme
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RookieApp(viewModel: RookieViewModel) {
  val state by viewModel.uiState.collectAsStateWithLifecycle()
  val isDark = when (state.settings.themeMode) {
    AppThemeMode.DARK -> true
    AppThemeMode.LIGHT -> false
    AppThemeMode.SYSTEM -> isSystemInDarkTheme()
  }

  BackHandler(
    enabled = state.activeTab != RookieTab.HOME || state.promotionPrompt != null || state.showSettingsSheet
  ) {
    viewModel.onBackPressed()
  }

  RookieTheme(darkTheme = isDark) {
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val showCopied: (String) -> Unit = { msg ->
      scope.launch {
        snackbarHostState.currentSnackbarData?.dismiss()
        snackbarHostState.showSnackbar(msg)
      }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val useWideLayout = maxWidth >= 700.dp

      Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
          CenterAlignedTopAppBar(
            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
              containerColor = MaterialTheme.colorScheme.surface,
              titleContentColor = MaterialTheme.colorScheme.onSurface,
            ),
            navigationIcon = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(start = 12.dp),
              ) {
                ChessPieceView(
                  piece = Piece(PieceType.KNIGHT, PieceColor.WHITE),
                  modifier = Modifier.size(28.dp),
                )
              }
            },
            title = {
              Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
              ) {
                Text(
                  text = stringResource(R.string.app_name),
                  style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                )
                Surface(
                  shape = RoundedCornerShape(8.dp),
                  color = MaterialTheme.colorScheme.surfaceVariant,
                  onClick = viewModel::toggleBeginnerMode,
                  modifier = Modifier.testTag("mode_pill_button"),
                ) {
                  Text(
                    text = if (state.settings.beginnerMode) "Beginner" else "Advanced",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                  )
                }
              }
            },
            actions = {
              IconButton(
                onClick = viewModel::toggleMuted,
                modifier = Modifier.testTag("mute_button"),
              ) {
                Icon(
                  imageVector = if (state.settings.muted) {
                    Icons.AutoMirrored.Rounded.VolumeOff
                  } else {
                    Icons.AutoMirrored.Rounded.VolumeUp
                  },
                  contentDescription = stringResource(R.string.cd_mute_toggle),
                )
              }
              IconButton(
                onClick = { viewModel.setShowSettings(true) },
                modifier = Modifier.testTag("settings_button"),
              ) {
                Icon(
                  imageVector = Icons.Rounded.Settings,
                  contentDescription = stringResource(R.string.cd_settings),
                )
              }
            },
          )
        },
        bottomBar = {
          if (!useWideLayout) {
            NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
              RookieTab.entries.forEach { tab ->
                val selected = state.activeTab == tab
                NavigationBarItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = {
                    Icon(
                      imageVector = tabIcon(tab, selected),
                      contentDescription = tab.label,
                    )
                  },
                  label = {
                    Text(
                      text = tab.label,
                      maxLines = 1,
                      overflow = TextOverflow.Ellipsis,
                    )
                  },
                  modifier = Modifier.testTag("nav_${tab.name.lowercase()}"),
                )
              }
            }
          }
        },
      ) { innerPadding ->
        if (useWideLayout) {
          Row(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding),
          ) {
            NavigationRail(containerColor = MaterialTheme.colorScheme.surface) {
              Spacer(Modifier.size(8.dp))
              RookieTab.entries.forEach { tab ->
                val selected = state.activeTab == tab
                NavigationRailItem(
                  selected = selected,
                  onClick = { viewModel.selectTab(tab) },
                  icon = {
                    Icon(
                      imageVector = tabIcon(tab, selected),
                      contentDescription = tab.label,
                    )
                  },
                  label = { Text(tab.label, maxLines = 1) },
                  modifier = Modifier.testTag("rail_${tab.name.lowercase()}"),
                )
              }
            }

            Row(
              modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
              horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
              BoxWithConstraints(
                modifier = Modifier
                  .weight(1.15f)
                  .fillMaxHeight(),
                contentAlignment = Alignment.TopCenter,
              ) {
                val maxBoardDim = (maxHeight - 84.dp).coerceAtMost(maxWidth).coerceAtLeast(220.dp)
                Column(
                  horizontalAlignment = Alignment.CenterHorizontally,
                  verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                  BoardSection(
                    state = state,
                    viewModel = viewModel,
                    modifier = Modifier.widthIn(max = maxBoardDim),
                  )
                }
              }

              Column(
                modifier = Modifier
                  .weight(1f)
                  .fillMaxHeight()
                  .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
              ) {
                ActivePanelContent(
                  state = state,
                  viewModel = viewModel,
                  onCopiedFeedback = showCopied,
                )
              }
            }
          }
        } else {
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(innerPadding)
              .verticalScroll(rememberScrollState())
              .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
          ) {
            BoardSection(
              state = state,
              viewModel = viewModel,
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 460.dp),
            )

            Box(
              modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 600.dp),
            ) {
              ActivePanelContent(
                state = state,
                viewModel = viewModel,
                onCopiedFeedback = showCopied,
              )
            }
          }
        }
      }

      if (state.showSettingsSheet) {
        SettingsSheet(
          settings = state.settings,
          onDismiss = { viewModel.setShowSettings(false) },
          onToggleAnimations = viewModel::toggleAnimations,
          onSelectSpeed = viewModel::selectAnimationSpeed,
          onToggleCaptureEffects = viewModel::toggleCaptureEffects,
          onToggleHighlights = viewModel::toggleHighlights,
          onToggleArrows = viewModel::toggleArrows,
          onToggleClearOnMove = viewModel::toggleClearOnMove,
          onToggleReducedMotion = viewModel::toggleReducedMotion,
          onToggleMuted = viewModel::toggleMuted,
          onVolumeChange = viewModel::setVolume,
          onToggleBeginnerMode = viewModel::toggleBeginnerMode,
          onSelectThemeMode = viewModel::selectThemeMode,
        )
      }
    }
  }
}

@Composable
private fun BoardSection(
  state: RookieUiState,
  viewModel: RookieViewModel,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    ChessBoard(
      board = state.board,
      flipped = state.flipped,
      selectedSquare = state.selectedSquare,
      legalTargets = state.legalTargets,
      lastMoveFrom = state.lastMoveFrom,
      lastMoveTo = state.lastMoveTo,
      checkSquare = state.inCheckSquare,
      hintFrom = state.hintFrom,
      hintTo = state.hintTo,
      drawMode = state.drawMode,
      drawColor = state.drawColor,
      drawStartSquare = state.drawStartSquare,
      drawings = state.drawings,
      highlightsEnabled = state.settings.highlightsEnabled,
      arrowsEnabled = state.settings.arrowsEnabled,
      captureEffectsEnabled = state.settings.captureEffects,
      animationDurationMs = state.settings.effectiveAnimationDurationMs,
      animationEvent = state.animationEvent,
      shakeEvent = state.shakeEvent,
      promotionPrompt = state.promotionPrompt,
      canInteractWithPiece = viewModel::canInteractWithPiece,
      onSquareTapped = viewModel::onSquareTapped,
      onPieceDragDrop = viewModel::onPieceDragDrop,
      onDrawGesture = viewModel::onDrawGesture,
      onPromotionSelected = viewModel::onPromotionSelected,
      onPromotionDismissed = viewModel::dismissPromotion,
      modifier = Modifier.fillMaxWidth(),
    )

    val maxHints = if (state.activeTab == RookieTab.LEARN || state.activeTab == RookieTab.PUZZLES) 4 else 0
    BoardControlsRow(
      showGameControls = state.activeTab != RookieTab.HOME,
      showRestart = state.activeTab == RookieTab.LEARN || state.activeTab == RookieTab.PUZZLES,
      canUndo = state.canUndo,
      canRedo = state.canRedo,
      hintCount = state.hintLevel,
      maxHints = maxHints,
      drawMode = state.drawMode,
      selectedDrawColor = state.drawColor,
      hasDrawings = state.hasDrawings,
      onFlip = viewModel::flipBoard,
      onUndo = viewModel::undo,
      onRedo = viewModel::redo,
      onHint = viewModel::showHint,
      onRestart = viewModel::restartCurrent,
      onToggleDrawMode = viewModel::toggleDrawMode,
      onSelectDrawColor = viewModel::selectDrawColor,
      onClearDrawings = viewModel::clearDrawings,
    )
  }
}

@Composable
private fun ActivePanelContent(
  state: RookieUiState,
  viewModel: RookieViewModel,
  onCopiedFeedback: (String) -> Unit,
) {
  when (state.activeTab) {
    RookieTab.HOME -> DashboardPanel(
      state = state,
      onNavigateTab = viewModel::selectTab,
    )
    RookieTab.LEARN -> LearnPanel(
      state = state,
      onSelectLesson = viewModel::selectLesson,
      onNextStep = viewModel::nextLessonStep,
    )
    RookieTab.PUZZLES -> PuzzlesPanel(
      state = state,
      onPreviousPuzzle = viewModel::previousPuzzle,
      onNextPuzzle = viewModel::nextPuzzle,
    )
    RookieTab.PLAY -> PlayAiPanel(
      state = state,
      onSelectLevel = viewModel::selectAiLevel,
      onNewGame = { color -> viewModel.startNewAiGame(color) },
      onResign = viewModel::resignAiGame,
      onCopiedFeedback = onCopiedFeedback,
    )
    RookieTab.SANDBOX -> SandboxPanel(
      state = state,
      onResetBoard = viewModel::restartCurrent,
      onCopiedFeedback = onCopiedFeedback,
    )
  }
}

private fun tabIcon(tab: RookieTab, selected: Boolean): ImageVector = when (tab) {
  RookieTab.HOME -> if (selected) Icons.Rounded.Dashboard else Icons.Outlined.Dashboard
  RookieTab.LEARN -> if (selected) Icons.Rounded.School else Icons.Outlined.School
  RookieTab.PUZZLES -> if (selected) Icons.Rounded.Extension else Icons.Outlined.Extension
  RookieTab.PLAY -> if (selected) Icons.Rounded.SmartToy else Icons.Outlined.SmartToy
  RookieTab.SANDBOX -> if (selected) Icons.Rounded.GridOn else Icons.Outlined.GridOn
}
