package tech.techyinc.vlc.mediadb.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity()
data class CustomDirectory(
        @PrimaryKey
        val path: String
)

