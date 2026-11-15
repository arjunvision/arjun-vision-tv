package com.arjunvision.tvlauncher.domain.themes

import com.arjunvision.tvlauncher.domain.models.Festival
import com.arjunvision.tvlauncher.domain.models.ThemeCategory
import java.time.LocalDate
import java.time.MonthDay
import java.time.temporal.ChronoUnit

/**
 * Indian and Tamil Nadu festival calendar used for automatic themes.
 *
 * Fixed-date festivals are computed for any year. Diwali follows the lunar calendar, so only the
 * years listed in [DIWALI_DATES] get a Diwali theme, and those dates are flagged for checking.
 */
object FestivalCalendar {

    /** Deepavali dates to verify against the Tamil Nadu government holiday list each year. */
    val DIWALI_DATES: Map<Int, LocalDate> = mapOf(
        2026 to LocalDate.of(2026, 11, 8),
        2027 to LocalDate.of(2027, 10, 29),
    )

    fun festivalsFor(year: Int): List<Festival> = buildList {
        val newYear = LocalDate.of(year, 1, 1)
        add(Festival("New Year", ThemeCategory.NEW_YEAR, newYear, newYear.minusDays(1), newYear.plusDays(1), false))

        // Bhogi (13 Jan) to Kaanum Pongal (16 Jan). Thai Pongal is 14 Jan in 2027 (the launch date).
        val pongal = LocalDate.of(year, 1, 14)
        add(Festival("Pongal", ThemeCategory.PONGAL, pongal, pongal.minusDays(1), pongal.plusDays(2), year != 2027))

        val puthandu = LocalDate.of(year, 4, 14)
        add(Festival("Tamil New Year", ThemeCategory.TAMIL_NEW_YEAR, puthandu, puthandu.minusDays(1), puthandu.plusDays(1), true))

        val independence = LocalDate.of(year, 8, 15)
        add(Festival("Independence Day", ThemeCategory.INDEPENDENCE_DAY, independence, independence.minusDays(1), independence.plusDays(1), false))

        DIWALI_DATES[year]?.let { diwali ->
            add(Festival("Diwali", ThemeCategory.DIWALI, diwali, diwali.minusDays(2), diwali.plusDays(1), true))
        }

        val christmas = LocalDate.of(year, 12, 25)
        add(Festival("Christmas", ThemeCategory.CHRISTMAS, christmas, christmas.minusDays(5), christmas.plusDays(1), false))
    }

    fun festivalsAround(date: LocalDate): List<Festival> =
        (date.year - 1..date.year + 1).flatMap(::festivalsFor).sortedBy { it.start }

    /** Tamil Nadu seasons: summer April-June, north-east monsoon mid-October to mid-December. */
    fun seasonFor(date: LocalDate): ThemeCategory? {
        val md = MonthDay.from(date)
        return when {
            date.monthValue in 4..6 -> ThemeCategory.SUMMER
            !md.isBefore(MonthDay.of(10, 15)) && !md.isAfter(MonthDay.of(12, 15)) -> ThemeCategory.MONSOON
            else -> null
        }
    }
}

data class ScheduleDecision(val category: ThemeCategory, val reason: String, val festival: Festival?)

data class UpcomingFestival(val festival: Festival, val daysAway: Long)

object ThemeScheduler {

    /** Which theme category belongs on screen on [date]: festival first, then season, then Classic. */
    fun decide(date: LocalDate): ScheduleDecision {
        FestivalCalendar.festivalsAround(date).firstOrNull { date in it.start..it.end }?.let {
            return ScheduleDecision(it.category, "${it.name} (${it.date})", it)
        }
        FestivalCalendar.seasonFor(date)?.let {
            return ScheduleDecision(it, "${it.title} season", null)
        }
        return ScheduleDecision(ThemeCategory.CLASSIC, "Everyday theme", null)
    }

    /** The next festival whose theme starts within [withinDays] days (for the 7-day preview). */
    fun upcoming(date: LocalDate, withinDays: Long = 7): UpcomingFestival? =
        FestivalCalendar.festivalsAround(date)
            .map { UpcomingFestival(it, ChronoUnit.DAYS.between(date, it.start)) }
            .filter { it.daysAway in 1..withinDays }
            .minByOrNull { it.daysAway }

    /** Categories worth preparing now: today's plus festivals starting in the next [days] days. */
    fun categoriesToPrepare(date: LocalDate, days: Long = 35): List<ThemeCategory> {
        val upcoming = FestivalCalendar.festivalsAround(date)
            .filter { ChronoUnit.DAYS.between(date, it.start) in 0..days }
            .map { it.category }
        return (listOf(decide(date).category) + upcoming).distinct()
    }
}
