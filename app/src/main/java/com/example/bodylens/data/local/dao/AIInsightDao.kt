package com.example.bodylens.data.local.dao

import androidx.room.*
import com.example.bodylens.data.model.AIInsight
import com.example.bodylens.data.model.InsightType
import kotlinx.coroutines.flow.Flow

@Dao
interface AIInsightDao {
    
    @Query("SELECT * FROM ai_insights WHERE entryId = :entryId ORDER BY timestamp DESC")
    fun getInsightsForEntry(entryId: Long): Flow<List<AIInsight>>
    
    @Query("SELECT * FROM ai_insights WHERE insightType = :type ORDER BY timestamp DESC LIMIT 10")
    fun getInsightsByType(type: InsightType): Flow<List<AIInsight>>
    
    @Query("SELECT * FROM ai_insights ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentInsights(limit: Int = 20): Flow<List<AIInsight>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsight(insight: AIInsight): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInsights(insights: List<AIInsight>)
    
    @Delete
    suspend fun deleteInsight(insight: AIInsight)
    
    @Query("DELETE FROM ai_insights WHERE entryId = :entryId")
    suspend fun deleteInsightsForEntry(entryId: Long)
}


