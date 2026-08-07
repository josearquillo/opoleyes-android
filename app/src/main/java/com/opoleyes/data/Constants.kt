package com.opoleyes.data

import com.opoleyes.data.model.Achievement
import com.opoleyes.data.model.Rank

object Constants {
    val RANKS = listOf(
        Rank("Novato", "🌱", 0, 0),
        Rank("Principiante", "🌿", 200, 1),
        Rank("Aprendiz", "📚", 800, 2),
        Rank("Estudiante", "📝", 2000, 3),
        Rank("Avanzado", "🔥", 4000, 4),
        Rank("Experto", "⚖️", 7000, 5),
        Rank("Veterano", "🎯", 12000, 6),
        Rank("Maestro", "👑", 18000, 7),
        Rank("Leyenda", "💎", 25000, 8),
    )

    val ACHIEVEMENTS = listOf(
        Achievement("first_correct", "🎯", "Primer acierto", "1 acierto"),
        Achievement("combo5", "🔥", "Combo x5", "5 aciertos seguidos"),
        Achievement("combo10", "🔥", "Combo x10", "10 aciertos seguidos"),
        Achievement("combo15", "⚡", "Combo x15", "15 aciertos seguidos"),
        Achievement("combo20", "🚀", "Imparable", "20 aciertos seguidos"),
        Achievement("combo25", "🌟", "Combo x25", "25 aciertos seguidos"),
        Achievement("100correct", "💯", "100 aciertos", "100 aciertos totales"),
        Achievement("500correct", "🔢", "500 aciertos", "500 aciertos totales"),
        Achievement("1000correct", "🏅", "Mil aciertos", "1000 aciertos totales"),
        Achievement("first_record", "🏆", "Primer récord", "Primer game over"),
        Achievement("medal_bronze", "🥉", "Medalla bronce", "300+ pts en una partida"),
        Achievement("medal_silver", "🥈", "Medalla plata", "600+ pts en una partida"),
        Achievement("medal_gold", "🥇", "Medalla oro", "1000+ pts en una partida"),
        Achievement("record_survival", "❤️", "Récord supervivencia", "Batir récord de Supervivencia"),
        Achievement("record_timetrial", "⏱️", "Récord contrarreloj", "Batir récord de Contrarreloj"),
        Achievement("record_quick", "⚡", "Récord repaso", "Batir récord de Repaso Express"),
        Achievement("dedicated", "🎮", "Jugador dedicado", "10 partidas jugadas"),
        Achievement("habitual", "🕹️", "Habitual", "25 partidas jugadas"),
        Achievement("addicted", "🤖", "Adicto", "50 partidas jugadas"),
        Achievement("expert", "⚖️", "Experto", "Alcanzar rango Experto"),
        Achievement("master", "👑", "Maestro", "Alcanzar rango Maestro"),
        Achievement("perfect_game", "🎯", "Diana perfecta", "100% acierto (mín. 10 preguntas)"),
        Achievement("sharpshooter", "📐", "Precisión quirúrgica", "90%+ acierto (mín. 10 preguntas)"),
        Achievement("strategist", "🎲", "Estratega", "Usar el 50/50 por primera vez"),
        Achievement("resurrection", "❤️", "Resurrección", "Recuperar una vida con la racha"),
        Achievement("first_law", "📖", "Primera ley", "Dominar 1 ley al 100%"),
        Achievement("five_laws", "📚", "Estudioso", "Dominar 5 leyes al 100%"),
        Achievement("ten_laws", "⚖️", "Jurista", "Dominar 10 leyes al 100%"),
        Achievement("all_laws", "👑", "Jurisconsulto", "Dominar todas las leyes al 100%"),
    )

    const val QUICK_MODE_QUESTIONS = 5

    val RANK_UNLOCKS = mapOf(
        3 to "⏱️ Contrarreloj",
        4 to "📋 2 misiones diarias",
        5 to "⚡ Repaso Express",
        6 to "📋 3 misiones diarias",
        7 to "📝 Mini Examen",
        8 to "🎯 Simulacro",
    )

    // Power-ups are now unlimited. No rank-based gifts needed.
    // Penalty for using power-ups is applied as a points multiplier on correct answers.

    // Mechanics per rank.
    val MAX_OPTIONS_BY_RANK = mapOf(
        0 to 4, 1 to 4, 2 to 4, 3 to 4, 4 to 4,
        5 to 4, 6 to 4, 7 to 4, 8 to 4
    )

    val MAX_LIVES_BY_RANK = mapOf(
        0 to 7, 1 to 5, 2 to 3, 3 to 3, 4 to 3,
        5 to 3, 6 to 3, 7 to 3, 8 to 3
    )

    val MAX_DIFFICULTY_BY_RANK = mapOf(
        0 to 2, 1 to 2, 2 to 3, 3 to 3, 4 to 4,
        5 to 4, 6 to 5, 7 to 5, 8 to 5
    )

    val STREAK_RECOVERY_THRESHOLD_BY_RANK = mapOf(
        0 to 3, 1 to 4, 2 to 5, 3 to 5, 4 to 5,
        5 to 5, 6 to 5, 7 to 5, 8 to 5
    )

    // Only 50/50 and Pista exist. Escudo and x2pts have been removed.
    // 50/50 is available from rank 0, Pista unlocks at rank 1.
    val AVAILABLE_POWERUPS_BY_RANK = mapOf(
        0 to listOf("fiftyFifty"),
        1 to listOf("fiftyFifty", "hint"),
        2 to listOf("fiftyFifty", "hint"),
        3 to listOf("fiftyFifty", "hint"),
        4 to listOf("fiftyFifty", "hint"),
        5 to listOf("fiftyFifty", "hint"),
        6 to listOf("fiftyFifty", "hint"),
        7 to listOf("fiftyFifty", "hint"),
        8 to listOf("fiftyFifty", "hint"),
    )

    // Points multiplier when a power-up was used on the correct answer.
    val POWERUP_POINTS_MULTIPLIER = mapOf(
        "hint" to 0.5f,      // Pista: 50% points
        "fiftyFifty" to 0.25f // 50/50: 25% points
    )

    fun getRankByIndex(index: Int): Rank = RANKS.getOrElse(index) { RANKS.last() }

    val LEY_GROUPS: List<Pair<String, IntRange>> = listOf(
        "Constitución Española" to (1..4),
        "Tribunal Constitucional" to (5..5),
        "Igualdad" to (6..7),
        "Ley del Gobierno y Régimen Jurídico del Sector Público" to (8..9),
        "Organización Territorial del Estado y Régimen Local" to (10..11),
        "Unión Europea" to (12..13),
        "Poder Judicial y CGPJ" to (14..14),
        "Jueces, Magistrados y Ministerio Fiscal (LOPJ I)" to (15..16),
        "Organización de Tribunales (LOPJ II)" to (17..18),
        "Tribunales de Instancia (LOPJ III)" to (19..20),
        "Jueces y Juezas de Paz" to (21..21),
        "Carta de Derechos Ciudadanos ante la Justicia" to (22..22),
        "Oficina Judicial y Protección de Datos" to (23..24),
        "Letrado de la Administración de Justicia" to (25..26),
        "Cuerpos al Servicio de la Administración de Justicia" to (27..27),
        "Cuerpos Generales I (Ingreso y TREBEP)" to (28..30),
        "Cuerpos Generales II (Situaciones Administrativas y Régimen Disciplinario)" to (31..32),
        "Libertad Sindical, Huelga y Prevención de Riesgos Laborales" to (33..33),
        "Cuestiones Generales del Proceso Civil" to (34..34),
        "Representación y sus Clases" to (35..35),
        "Jurisdicción y Competencia Civil" to (36..36),
        "Actuaciones Judiciales" to (37..42),
        "Archivo Judicial" to (43..43),
        "Diligencias Preliminares y Prueba Anticipada" to (44..45),
        "Juicio Ordinario" to (46..48),
        "Juicio Verbal" to (49..50),
        "Procesos División Judicial de Patrimonios" to (51..51),
        "Monitorio y Cambiario" to (52..52),
        "Procesos Matrimoniales" to (53..54),
        "Jurisdicción Voluntaria" to (55..57),
        "Recursos Civiles" to (58..58),
        "Recursos Civiles Extraordinarios" to (59..59),
        "Ejecución Forzosa" to (60..61),
        "Ejecución Dineraria" to (62..63),
        "Procedimiento de Apremio" to (64..65),
        "Ejecución No Dineraria" to (66..66),
        "Medidas Cautelares en Civil" to (67..67),
        "Tasación de Costas Civil" to (68..68),
        "Registro Civil (Ley 2011) - Títulos" to (69..70),
        "Publicidad del Registro Civil" to (71..71),
        "Sistema Procesal Penal" to (72..72),
        "Partes en el Proceso Penal" to (73..73),
        "El Sumario" to (74..74),
        "Medidas Cautelares Penales" to (75..76),
        "Conclusión del Sumario" to (77..77),
        "Prueba en Penal" to (78..78),
        "Procedimiento Abreviado (Penal)" to (79..81),
        "Procedimiento de Enjuiciamiento Rápido" to (82..82),
        "Tribunal del Jurado" to (83..83),
        "Tutela Judicial ante el Juzgado VSM" to (84..84),
        "Responsabilidad Penal del Menor" to (85..85),
        "Juicio de Delito Leve" to (86..86),
        "Recursos Penales" to (87..87),
        "Ejecución Penal" to (88..88),
        "Organización de la Jurisdicción Contencioso-Administrativa" to (89..89),
        "Contencioso-Administrativa: Capacidad" to (90..90),
        "Contencioso-Administrativa: Diligencias Preliminares" to (91..91),
        "Contencioso-Administrativa: Demanda y Contestación" to (92..92),
        "Contencioso-Administrativa: Procedimiento Abreviado" to (93..93),
        "Recursos en el Proceso Contencioso-Administrativo" to (94..94),
        "Procesos Especiales Contencioso-Administrativos" to (95..95),
        "Disposiciones Comunes Contencioso-Administrativas" to (96..96),
        "Social: Principios Laborales" to (97..97),
        "Social: Evitación del Proceso" to (98..98),
        "Social: Procesos Especiales y Ejecución" to (99..99),
        "Concursal" to (100..101),
    )

    fun getLeyForTest(testName: String): String {
        val num = testName.removePrefix("Tema N").toIntOrNull() ?: return testName
        return LEY_GROUPS.firstOrNull { num in it.second }?.first ?: testName
    }
}
