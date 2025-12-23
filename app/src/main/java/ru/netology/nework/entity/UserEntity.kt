package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.User

@Entity
data class UserEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val login: String,
    val avatar: String?
) {
    fun toUserDto() = User(
        id,
        name,
        login,
        avatar
    )

    companion object {
        fun fromUserDto(user: User) = UserEntity(
            user.id,
            user.name,
            user.login,
            user.avatar
        )
    }
}

fun List<UserEntity>.toUserDto(): List<User> = map(UserEntity::toUserDto)
fun List<User>.toUserEntity(): List<UserEntity> = map(UserEntity::fromUserDto)