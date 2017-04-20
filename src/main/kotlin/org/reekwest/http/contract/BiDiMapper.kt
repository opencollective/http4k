package org.reekwest.http.contract

import org.reekwest.http.asByteBuffer
import org.reekwest.http.asString
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets.UTF_8

interface BiDiMapper<IN, OUT> {
    fun mapIn(source: IN): OUT
    fun mapOut(source: OUT): IN

    companion object {
        fun <T> Identity() = object : BiDiMapper<T, T> {
            override fun mapIn(source: T): T = source
            override fun mapOut(source: T): T = source
        }
    }
}

object ByteBufferStringBiDiMapper : BiDiMapper<ByteBuffer, String> {
    override fun mapIn(source: ByteBuffer): String = source.asString()
    override fun mapOut(source: String): ByteBuffer = source.asByteBuffer()
}

fun <NEXT, OUT> BiDiMapper<ByteBuffer, OUT>.map(nextIn: (OUT) -> NEXT): BiDiMapper<ByteBuffer, NEXT> =
    object : BiDiMapper<ByteBuffer, NEXT> {
        override fun mapIn(source: ByteBuffer): NEXT = nextIn(this@map.mapIn(source))
        override fun mapOut(source: NEXT): ByteBuffer = UTF_8.encode(source.toString())
    }

fun <NEXT, OUT> BiDiMapper<ByteBuffer, OUT>.map(nextIn: (OUT) -> NEXT, nextOut: (NEXT) -> OUT): BiDiMapper<ByteBuffer, NEXT> =
    object : BiDiMapper<ByteBuffer, NEXT> {
        override fun mapIn(source: ByteBuffer): NEXT = nextIn(this@map.mapIn(source))
        override fun mapOut(source: NEXT): ByteBuffer = this@map.mapOut(nextOut(source))
    }