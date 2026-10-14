package com.sunnahcompanion.app.data.repository

import com.sunnahcompanion.app.core.database.dao.SunnahItemDao
import com.sunnahcompanion.app.core.database.model.ContentKind
import com.sunnahcompanion.app.core.database.model.LifeSituation
import com.sunnahcompanion.app.data.model.ContentItem
import com.sunnahcompanion.app.data.model.asModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

@Singleton
class ContentRepository @Inject constructor(
    private val sunnahItemDao: SunnahItemDao,
) {
    fun observeSituation(situation: LifeSituation): Flow<List<ContentItem>> =
        sunnahItemDao.observeBySituation(situation).map { items -> items.map { it.asModel() } }

    fun observeKind(kind: ContentKind): Flow<List<ContentItem>> =
        sunnahItemDao.observeByKind(kind).map { items -> items.map { it.asModel() } }

    fun observeItem(id: String): Flow<ContentItem?> =
        sunnahItemDao.observeItemWithDetails(id).map { it?.asModel() }

    suspend fun getItem(id: String): ContentItem? = sunnahItemDao.getItemWithDetails(id)?.asModel()

    fun search(query: String): Flow<List<ContentItem>> =
        sunnahItemDao.search(query).map { items -> items.map { it.asModel() } }
}
