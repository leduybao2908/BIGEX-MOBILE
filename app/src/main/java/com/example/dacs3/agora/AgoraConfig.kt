package com.example.dacs3.agora

object AgoraConfig {
    // Thay thế bằng App ID từ Agora Console
    const val APP_ID = "544675968c194a4a9e2eccd954f91326"
    
    // URL của server để lấy token (nếu cần)
    const val TOKEN_SERVER_URL = "your-token-server-url"
    
    // Thời gian timeout cho việc kết nối (ms)
    const val CONNECTION_TIMEOUT = 10000L
    
    // Cấu hình mặc định cho video
    object VideoConfig {
        const val FRAME_RATE = 15
        const val DIMENSION_WIDTH = 640
        const val DIMENSION_HEIGHT = 360
        const val BITRATE = 800 // kbps
    }
}