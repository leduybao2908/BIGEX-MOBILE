package com.example.dacs3.data

fun UserDatabaseModel.toUser(): User {
    return User(
        uid = this.uid,
        username = this.username,
        email = this.email,
        name = this.fullName,
        profilePicture = this.profilePicture,
        isOnline = this.isOnline,
        lastOnline = this.lastOnline,
        // Các trường này không có trong UserDatabaseModel, nên gán giá trị mặc định
        password = "",
        birthdate = "",
        phone = "",
        interests = emptyList(),
        city = null
    )
}

fun User.toUserDatabaseModel(): UserDatabaseModel {
    return UserDatabaseModel(
        uid = this.uid,
        username = this.username,
        email = this.email,
        fullName = this.name,
        profilePicture = this.profilePicture,
        isOnline = this.isOnline,
        lastOnline = this.lastOnline
        // createdAt và fcmToken có thể giữ giá trị mặc định trong UserDatabaseModel
    )
}