package com.opoleyes.ui.navigation

import android.content.Context
import androidx.test.core.app.ApplicationProvider

object TestStrings {
    private val ctx = ApplicationProvider.getApplicationContext<Context>()

    // Common
    val back = ctx.getString(com.opoleyes.R.string.back)
    val cancel = ctx.getString(com.opoleyes.R.string.cancel)
    val error = ctx.getString(com.opoleyes.R.string.error)
    val menu = ctx.getString(com.opoleyes.R.string.menu)
    val playAgain = ctx.getString(com.opoleyes.R.string.play_again)
    val continueLabel = ctx.getString(com.opoleyes.R.string.continue_label)
    val points = ctx.getString(com.opoleyes.R.string.points)
    val questionsLabel = ctx.getString(com.opoleyes.R.string.questions_label)
    val maxComboLabel = ctx.getString(com.opoleyes.R.string.max_combo_label)
    val accuracyLabel = ctx.getString(com.opoleyes.R.string.accuracy_label)
    val gameOver = ctx.getString(com.opoleyes.R.string.game_over)

    // Home
    val play = ctx.getString(com.opoleyes.R.string.play)

    // Mode select
    val selectMode = ctx.getString(com.opoleyes.R.string.select_mode)
    val modeSurvival = ctx.getString(com.opoleyes.R.string.mode_survival)
    val modeTimetrial = ctx.getString(com.opoleyes.R.string.mode_timetrial)
    val modeQuick = ctx.getString(com.opoleyes.R.string.mode_quick)
    val modeExam = ctx.getString(com.opoleyes.R.string.mode_exam)
    val modeSimulacro = ctx.getString(com.opoleyes.R.string.mode_simulacro)

    // Tema select
    val selectLaw = ctx.getString(com.opoleyes.R.string.select_law)
    val searchLaw = ctx.getString(com.opoleyes.R.string.search_law)
    val allLaws = ctx.getString(com.opoleyes.R.string.all_laws)

    // Help
    val help = ctx.getString(com.opoleyes.R.string.help)
    val helpObjective = ctx.getString(com.opoleyes.R.string.help_section_objective_title)
    val helpModes = ctx.getString(com.opoleyes.R.string.help_section_modes_title)
    val helpPowerups = ctx.getString(com.opoleyes.R.string.help_section_powerups_title)
    val helpCombo = ctx.getString(com.opoleyes.R.string.help_section_combo_title)
    val helpRanks = ctx.getString(com.opoleyes.R.string.help_section_ranks_title)
    val helpBonus = ctx.getString(com.opoleyes.R.string.help_section_bonus_title)
    val helpMissions = ctx.getString(com.opoleyes.R.string.help_section_missions_title)

    // Profile
    val profile = ctx.getString(com.opoleyes.R.string.profile)
    val records = ctx.getString(com.opoleyes.R.string.records)
    val achievements = ctx.getString(com.opoleyes.R.string.achievements, 0, 0)
    val statistics = ctx.getString(com.opoleyes.R.string.statistics)
    val resetProgress = ctx.getString(com.opoleyes.R.string.reset_progress)
    val reset = ctx.getString(com.opoleyes.R.string.reset)

    // Loading
    val loadingApp = ctx.getString(com.opoleyes.R.string.loading_app)

    // Exam result
    val reviewAnswers = ctx.getString(com.opoleyes.R.string.review_answers)
    val resultados = ctx.getString(com.opoleyes.R.string.resultados)
    val examResult = ctx.getString(com.opoleyes.R.string.exam_result)
    val retryLabel = ctx.getString(com.opoleyes.R.string.retry_label)
    val hideReview = ctx.getString(com.opoleyes.R.string.hide_review)

    // Exam screen
    val exit = ctx.getString(com.opoleyes.R.string.exit)
    val finish = ctx.getString(com.opoleyes.R.string.finish)
    val next = ctx.getString(com.opoleyes.R.string.next)
    val previous = ctx.getString(com.opoleyes.R.string.previous)
    val exitExam = ctx.getString(com.opoleyes.R.string.exit_exam)
    val finishExam = ctx.getString(com.opoleyes.R.string.finish_exam)

    // Game screen
    val backToMenu = ctx.getString(com.opoleyes.R.string.back_to_menu)

    // Simulacro intro
    val simulacroStart = ctx.getString(com.opoleyes.R.string.simulacro_start)
    val simulacroIntroTitle = ctx.getString(com.opoleyes.R.string.simulacro_intro_title)

    // Mode intro
    val introPlay = ctx.getString(com.opoleyes.R.string.intro_play)
    val introDontShowAgain = ctx.getString(com.opoleyes.R.string.intro_dont_show_again)

    // Error screen
    val retry = ctx.getString(com.opoleyes.R.string.retry)

    // Quick reward dialog
    val quickRewardStart = ctx.getString(com.opoleyes.R.string.quick_reward_start)

    // Game screen
    val hint = ctx.getString(com.opoleyes.R.string.hint)
    val fiftyFifty = ctx.getString(com.opoleyes.R.string.fifty_fifty)

    // Chest overlay
    val tapToOpen = ctx.getString(com.opoleyes.R.string.tap_to_open)

    // Empty states
    val noResults = ctx.getString(com.opoleyes.R.string.no_results)
}
