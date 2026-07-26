package com.opotest.data

import com.opotest.data.model.Achievement
import com.opotest.data.model.Rank

object Constants {
    val RANKS = listOf(
        Rank("Novato", "🌱", 0, 0),
        Rank("Principiante", "🌿", 500, 1),
        Rank("Aprendiz", "📚", 1500, 2),
        Rank("Avanzado", "🔥", 3500, 3),
        Rank("Experto", "⚖️", 7000, 4),
        Rank("Veterano", "🎯", 12000, 5),
        Rank("Maestro", "👑", 20000, 6),
        Rank("Gran Maestro", "💎", 30000, 7),
        Rank("Élite", "⚡", 45000, 8),
        Rank("Campeón", "🏅", 60000, 9),
        Rank("Inmortal", "🛡️", 80000, 10),
        Rank("Leyenda", "🏆", 100000, 11),
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
        Achievement("studious", "📚", "Estudioso", "Completar 5 entrenamientos"),
        Achievement("student", "✏️", "Estudiante aplicado", "10 entrenamientos completados"),
        Achievement("professor", "🎓", "Catedrático", "25 entrenamientos completados"),
        Achievement("expert", "⚖️", "Experto", "Alcanzar rango Experto"),
        Achievement("master", "👑", "Maestro", "Alcanzar rango Maestro"),
        Achievement("grandmaster", "💎", "Gran Maestro", "Alcanzar rango Gran Maestro"),
        Achievement("elite", "⚡", "Élite", "Alcanzar rango Élite"),
        Achievement("legend", "🏆", "Leyenda", "Alcanzar rango Leyenda"),
        Achievement("perfect_game", "🎯", "Diana perfecta", "100% acierto (mín. 10 preguntas)"),
        Achievement("sharpshooter", "📐", "Precisión quirúrgica", "90%+ acierto (mín. 10 preguntas)"),
        Achievement("strategist", "🎲", "Estratega", "Usar el 50/50 por primera vez"),
        Achievement("resurrection", "❤️", "Resurrección", "Recuperar una vida con la racha"),
        Achievement("first_law", "📖", "Primera ley", "Dominar 1 ley al 100%"),
        Achievement("five_laws", "📚", "Estudioso", "Dominar 5 leyes al 100%"),
        Achievement("ten_laws", "⚖️", "Jurista", "Dominar 10 leyes al 100%"),
        Achievement("all_laws", "👑", "Jurisconsulto", "Dominar todas las leyes al 100%"),
    )

    const val QUICK_MODE_QUESTIONS = 20

    val RANK_UNLOCKS = mapOf(
        1 to "🛡️ Escudo + ⏱️ Contrarreloj",
        2 to "⚡ Repaso Express + 2 misiones",
        3 to "🎯 50/50",
        4 to "🏆 Modo Reto + ❤️ Recuperación de vida",
        5 to "✨ Double Score",
        6 to "🧊 Freeze Time",
        8 to "📋 3 misiones diarias",
    )

    fun getRankByIndex(index: Int): Rank = RANKS.getOrElse(index) { RANKS.last() }
}
