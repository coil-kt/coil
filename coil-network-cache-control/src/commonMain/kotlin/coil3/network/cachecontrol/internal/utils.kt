/*
 * Copyright (C) 2019 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package coil3.network.cachecontrol.internal

import kotlinx.datetime.UtcOffset
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.DateTimeComponents.Companion.Format
import kotlinx.datetime.format.DateTimeFormat
import kotlinx.datetime.format.DayOfWeekNames
import kotlinx.datetime.format.MonthNames
import kotlinx.datetime.format.Padding
import kotlinx.datetime.format.alternativeParsing
import kotlinx.datetime.format.char
import kotlinx.datetime.format.optional

internal fun String?.toNonNegativeInt(defaultValue: Int): Int {
    try {
        val value = this?.toLong() ?: return defaultValue
        return when {
            value > Int.MAX_VALUE -> Int.MAX_VALUE
            value < 0 -> 0
            else -> value.toInt()
        }
    } catch (_: NumberFormatException) {
        return defaultValue
    }
}

/**
 * Returns the next index in this at or after [startIndex] that is a character from
 * [characters]. Returns the input length if none of the requested characters can be found.
 */
internal fun String.indexOfElement(
    characters: String,
    startIndex: Int = 0,
): Int {
    for (i in startIndex until length) {
        if (this[i] in characters) {
            return i
        }
    }
    return length
}

/**
 * Returns the index of the next non-whitespace character in this. Result is undefined if input
 * contains newline characters.
 */
internal fun String.indexOfNonWhitespace(startIndex: Int = 0): Int {
    for (i in startIndex until length) {
        val c = this[i]
        if (c != ' ' && c != '\t') {
            return i
        }
    }
    return length
}

/**
 * A custom [DateTimeFormat] that matches all of the following date formats:
 *
 * // HTTP formats required by RFC2616 but with any timezone:
 * // RFC 822, updated by RFC 1123 with any TZ.
 * "EEE, dd MMM yyyy HH:mm:ss zzz"
 * // RFC 850, obsoleted by RFC 1036 with any TZ.
 * "EEEE, dd-MMM-yy HH:mm:ss zzz"
 * // ANSI C's asctime() format
 * "EEE MMM d HH:mm:ss yyyy"
 * // Alternative formats:
 * "EEE, dd-MMM-yyyy HH:mm:ss z"
 * "EEE, dd-MMM-yyyy HH-mm-ss z"
 * "EEE, dd MMM yy HH:mm:ss z"
 * "EEE dd-MMM-yyyy HH:mm:ss z"
 * "EEE dd MMM yyyy HH:mm:ss z"
 * "EEE dd-MMM-yyyy HH-mm-ss z"
 * "EEE dd-MMM-yy HH:mm:ss z"
 * "EEE dd MMM yy HH:mm:ss z"
 * "EEE,dd-MMM-yy HH:mm:ss z"
 * "EEE,dd-MMM-yyyy HH:mm:ss z"
 * "EEE, dd-MM-yyyy HH:mm:ss z"
 */
internal val BROWSER_DATE_TIME_FORMAT: DateTimeFormat<DateTimeComponents> = Format {
    // EEE
    alternativeParsing({ /* The day of the week is optional. */ }) {
        dayOfWeek(DayOfWeekNames.ENGLISH_ABBREVIATED)
        alternativeParsing({ char(',') }) {
            chars(", ")
        }
    }

    // dd
    dayOfMonth(Padding.NONE)
    alternativeParsing({ char('-') }) {
        char(' ')
    }

    // MM/MMM
    alternativeParsing({ dayOfMonth() }) {
        monthName(MonthNames.ENGLISH_ABBREVIATED)
    }
    alternativeParsing({ char('-') }) {
        char(' ')
    }

    // yy/yyyy
    alternativeParsing({ yearTwoDigits(1970) }) {
        year()
    }
    char(' ')

    // HH
    hour()
    char(':')

    // mm
    minute()

    // ss
    optional(":0") {
        char(':')
        second()
    }
    chars(" ")

    // z
    alternativeParsing({
        chars("UT")
    }, {
        chars("Z")
    }) {
        optional("GMT") {
            offset(UtcOffset.Formats.FOUR_DIGITS)
        }
    }
}
