package ru.adel.incidentstrackerandroid

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ViewModelComponent
import ru.adel.incidentstrackerandroid.repository.AuthRepository
import ru.adel.incidentstrackerandroid.repository.MainRepository
import ru.adel.incidentstrackerandroid.service.auth.AuthApiService
import ru.adel.incidentstrackerandroid.service.main.MainApiService

@Module
@InstallIn(ViewModelComponent::class)
class HiltModule {

    @Provides
    fun provideAuthRepository(authApiService: AuthApiService) = AuthRepository(authApiService)

    @Provides
    fun provideMainRepository(mainApiService: MainApiService) = MainRepository(mainApiService)
}