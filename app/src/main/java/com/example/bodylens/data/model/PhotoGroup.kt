package com.example.bodylens.data.model

enum class PhotoGroup(val displayName: String) {
    FACE("Face"),
    FRONT("Front View"),
    BACK("Back View"),
    SIDE_LEFT("Left Side"),
    SIDE_RIGHT("Right Side"),
    CUSTOM("Custom Angle");
    
    companion object {
        fun fromString(value: String): PhotoGroup {
            return entries.find { it.name == value } ?: CUSTOM
        }
    }
}


