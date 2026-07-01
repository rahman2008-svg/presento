package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "presentations")
data class PresentationEntity(
    @PrimaryKey val id: String,
    val title: String,
    val slidesJson: String,
    val commentsJson: String,
    val lastModified: Long,
    val isCloudSynced: Boolean
) {
    fun toPresentation(): Presentation {
        return Presentation(
            id = id,
            title = title,
            slides = JsonConverter.deserializeSlides(slidesJson),
            lastModified = lastModified,
            isCloudSynced = isCloudSynced,
            comments = JsonConverter.deserializeComments(commentsJson)
        )
    }

    companion object {
        fun fromPresentation(p: Presentation): PresentationEntity {
            return PresentationEntity(
                id = p.id,
                title = p.title,
                slidesJson = JsonConverter.serializeSlides(p.slides),
                commentsJson = JsonConverter.serializeComments(p.comments),
                lastModified = p.lastModified,
                isCloudSynced = p.isCloudSynced
            )
        }
    }
}
