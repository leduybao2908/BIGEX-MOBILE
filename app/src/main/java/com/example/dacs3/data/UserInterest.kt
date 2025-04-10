package com.example.dacs3.data

enum class UserInterest {
    SPORTS,
    MUSIC,
    TRAVEL,
    READING,
    COOKING,
    PHOTOGRAPHY,
    GAMING,
    MOVIES,
    ART,
    TECHNOLOGY;

    override fun toString(): String {
        return when (this) {
            SPORTS -> "Thể thao"
            MUSIC -> "Âm nhạc"
            TRAVEL -> "Du lịch"
            READING -> "Đọc sách"
            COOKING -> "Nấu ăn"
            PHOTOGRAPHY -> "Nhiếp ảnh"
            GAMING -> "Chơi game"
            MOVIES -> "Xem phim"
            ART -> "Nghệ thuật"
            TECHNOLOGY -> "Công nghệ"
        }
    }
}