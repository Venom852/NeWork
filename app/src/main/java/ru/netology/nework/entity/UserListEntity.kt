package ru.netology.nework.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nework.dto.User

@Entity
data class UserListEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long,
    val name: String,
    val login: String,
    val avatar: String?
) {
    fun toUserListDto() = User(
        id,
        name,
        login,
        avatar
    )

    companion object {
        fun fromUserListDto(user: User) = UserListEntity(
            user.id,
            user.name,
            user.login,
            user.avatar
        )
    }
}

fun List<UserListEntity>.toUserListDto(): List<User> = map(UserListEntity::toUserListDto)
fun List<User>.toUserListEntity(): List<UserListEntity> = map(UserListEntity::fromUserListDto)