package com.example.data.api

import com.squareup.moshi.FromJson
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.ToJson

/**
 * Adaptadores tolerantes para tipos numéricos y booleanos.
 * Permiten procesar respuestas de APIs que puedan enviar enteros, doubles
 * o strings sin causar excepciones de deserialización.
 */
class FlexibleLongAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Long {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextLong()
            JsonReader.Token.STRING -> {
                val str = reader.nextString()
                str.toLongOrNull() ?: str.hashCode().toLong()
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                0L
            }
            else -> {
                reader.skipValue()
                0L
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Long) {
        writer.value(value)
    }
}

class FlexibleDoubleAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Double {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextDouble()
            JsonReader.Token.STRING -> {
                val str = reader.nextString().replace(",", ".")
                str.toDoubleOrNull() ?: 0.0
            }
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                0.0
            }
            else -> {
                reader.skipValue()
                0.0
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Double) {
        writer.value(value)
    }
}

class FlexibleIntAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Int {
        return when (reader.peek()) {
            JsonReader.Token.NUMBER -> reader.nextInt()
            JsonReader.Token.STRING -> reader.nextString().toIntOrNull() ?: 0
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                0
            }
            else -> {
                reader.skipValue()
                0
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Int) {
        writer.value(value)
    }
}

class FlexibleBooleanAdapter {
    @FromJson
    fun fromJson(reader: JsonReader): Boolean {
        return when (reader.peek()) {
            JsonReader.Token.BOOLEAN -> reader.nextBoolean()
            JsonReader.Token.STRING -> {
                val str = reader.nextString().trim().lowercase()
                str == "true" || str == "1" || str == "yes" || str == "si"
            }
            JsonReader.Token.NUMBER -> reader.nextInt() != 0
            JsonReader.Token.NULL -> {
                reader.nextNull<Unit>()
                false
            }
            else -> {
                reader.skipValue()
                false
            }
        }
    }

    @ToJson
    fun toJson(writer: JsonWriter, value: Boolean) {
        writer.value(value)
    }
}
