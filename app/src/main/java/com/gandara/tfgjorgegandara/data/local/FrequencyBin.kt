package com.gandara.tfgjorgegandara.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "frequency_bins",
    primaryKeys = ["sampleId", "band"],
    foreignKeys = [
        ForeignKey(
            entity = AudioSample::class,
            parentColumns = ["id"],
            childColumns = ["sampleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sampleId"]), Index(value = ["band"])]
)
data class FrequencyBin(
    val sampleId: Long,
    val band: Int,    // Índice de la banda de frecuencia (0-31)
    val energy: Float // Valor de dB o magnitud normalizada
)
