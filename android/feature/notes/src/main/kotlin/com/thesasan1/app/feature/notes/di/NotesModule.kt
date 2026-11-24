package com.thesasan1.app.feature.notes.di

import com.thesasan1.app.feature.notes.data.OfflineNoteRepository
import com.thesasan1.app.feature.notes.domain.NoteRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NotesModule {
    @Binds fun bindNoteRepository(repository: OfflineNoteRepository): NoteRepository
}
