package com.example.dacs3.data

enum class VietnamCity {
    HANOI,
    HO_CHI_MINH_CITY,
    DA_NANG,
    HAI_PHONG,
    CAN_THO,
    BIEN_HOA,
    HUE,
    NHA_TRANG,
    VINH,
    QUY_NHON;

    override fun toString(): String {
        return when (this) {
            HANOI -> "Hà Nội"
            HO_CHI_MINH_CITY -> "TP. Hồ Chí Minh"
            DA_NANG -> "Đà Nẵng"
            HAI_PHONG -> "Hải Phòng"
            CAN_THO -> "Cần Thơ"
            BIEN_HOA -> "Biên Hòa"
            HUE -> "Huế"
            NHA_TRANG -> "Nha Trang"
            VINH -> "Vinh"
            QUY_NHON -> "Quy Nhơn"
        }
    }
}