package com.example.whispry.features.expander.data.repository

import com.example.whispry.features.expander.data.local.db.TextExpanderDao
import com.example.whispry.features.expander.data.model.TextExpanderEntity
import com.example.whispry.features.expander.domain.repository.TextExpanderRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TextExpanderRepositoryImpl @Inject constructor(
    private val textExpanderDao: TextExpanderDao
) : TextExpanderRepository {
    override fun getAllExpanders(): Flow<List<TextExpanderEntity>> = textExpanderDao.getAllExpandersFlow()

    override suspend fun getExpansionForShortcut(shortcut: String): String? {
        return textExpanderDao.getExpanderByShortcut(shortcut.trim().lowercase())?.expansion
    }

    override suspend fun saveExpander(shortcut: String, expansion: String) {
        val entity = TextExpanderEntity(
            shortcut = shortcut.trim().lowercase(),
            expansion = expansion
        )
        textExpanderDao.insertExpander(entity)
    }

    override suspend fun deleteExpander(expander: TextExpanderEntity) {
        textExpanderDao.deleteExpander(expander)
    }
}
