package com.gandara.tfgjorgegandara.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "sound_classifications",
    primaryKeys = ["sampleId", "label"],
    foreignKeys = [
        ForeignKey(
            entity = AudioSample::class,
            parentColumns = ["id"],
            childColumns = ["sampleId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["sampleId"])]
)
data class SoundClassification(
    val sampleId: Long,
    val label: String,      // Ej: "Tráfico", "Sirena", "Gente"
    val probability: Float  // Confianza de YAMNet (0.0 a 1.0)
)
