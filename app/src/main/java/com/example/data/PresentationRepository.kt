package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PresentationRepository(private val presentationDao: PresentationDao) {

    val allPresentations: Flow<List<Presentation>> = presentationDao.getAllPresentations()
        .map { entities ->
            entities.map { it.toPresentation() }
        }

    suspend fun getPresentationById(id: String): Presentation? {
        return presentationDao.getPresentationById(id)?.toPresentation()
    }

    suspend fun savePresentation(presentation: Presentation) {
        presentationDao.insertPresentation(PresentationEntity.fromPresentation(presentation))
    }

    suspend fun deletePresentationById(id: String) {
        presentationDao.deletePresentationById(id)
    }

    suspend fun clearAll() {
        presentationDao.deleteAllPresentations()
    }
}
