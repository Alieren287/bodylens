package com.example.bodylens.data.local

import androidx.room.TypeConverter
import com.example.bodylens.data.model.AnalysisStatus
import com.example.bodylens.data.model.InsightType
import com.example.bodylens.data.model.MeasurementType
import com.example.bodylens.data.model.PhotoGroup

class Converters {
    @TypeConverter
    fun fromPhotoGroup(value: PhotoGroup): String = value.name
    
    @TypeConverter
    fun toPhotoGroup(value: String): PhotoGroup = PhotoGroup.fromString(value)
    
    @TypeConverter
    fun fromAnalysisStatus(value: AnalysisStatus): String = value.name
    
    @TypeConverter
    fun toAnalysisStatus(value: String): AnalysisStatus = 
        AnalysisStatus.valueOf(value)
    
    @TypeConverter
    fun fromMeasurementType(value: MeasurementType): String = value.name
    
    @TypeConverter
    fun toMeasurementType(value: String): MeasurementType = 
        MeasurementType.valueOf(value)
    
    @TypeConverter
    fun fromInsightType(value: InsightType): String = value.name
    
    @TypeConverter
    fun toInsightType(value: String): InsightType = 
        InsightType.valueOf(value)
}


