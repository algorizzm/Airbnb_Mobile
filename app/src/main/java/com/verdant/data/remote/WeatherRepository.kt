package com.verdant.data.remote

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.URL

data class WeatherState(
    val tempC: Int,
    val condition: String,
    val emoji: String,
    val isGoodForHiking: Boolean
)

object WeatherRepository {

    // WMO weather interpretation codes → human label + emoji
    // https://open-meteo.com/en/docs#weathervariables
    private fun interpret(code: Int): Pair<String, String> = when (code) {
        0            -> "Clear skies"      to "☀️"
        1            -> "Mostly clear"     to "🌤️"
        2            -> "Partly cloudy"    to "⛅"
        3            -> "Overcast"         to "☁️"
        in 45..48    -> "Foggy"            to "🌫️"
        in 51..55    -> "Drizzle"          to "🌦️"
        in 61..65    -> "Rain"             to "🌧️"
        in 71..77    -> "Snow"             to "❄️"
        in 80..82    -> "Rain showers"     to "🌧️"
        in 85..86    -> "Snow showers"     to "🌨️"
        in 95..99    -> "Thunderstorm"     to "⛈️"
        else         -> "Cloudy"           to "☁️"
    }

    private fun goodForHiking(code: Int) = code in setOf(0, 1, 2, 3)

    /**
     * Fetch current weather from Open-Meteo (no API key required).
     * Falls back gracefully on any error.
     */
    suspend fun fetch(lat: Double, lon: Double): Result<WeatherState> =
        withContext(Dispatchers.IO) {
            runCatching {
                val url = "https://api.open-meteo.com/v1/forecast" +
                        "?latitude=$lat&longitude=$lon" +
                        "&current_weather=true" +
                        "&temperature_unit=celsius" +
                        "&timezone=auto"

                val json = URL(url).readText()
                val current = JSONObject(json).getJSONObject("current_weather")
                val temp  = current.getDouble("temperature").toInt()
                val code  = current.getInt("weathercode")
                val (condition, emoji) = interpret(code)

                WeatherState(
                    tempC          = temp,
                    condition      = condition,
                    emoji          = emoji,
                    isGoodForHiking = goodForHiking(code)
                )
            }
        }
}