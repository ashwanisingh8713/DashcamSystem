package cam.et.dashcamsystem.di

import cam.et.dashcamcore.domain.repo.IUserRepository
import cam.et.dashcamcore.domain.usecase.login.SignInUseCase
import cam.et.dashcamcore.domain.usecase.login.SignUpUseCase
import cam.et.dashcamcore.repository.ILoginRepo
import cam.et.dashcamcore.repository.IProfileRepo
import cam.et.dashcamcore.repository.UserRepositoryImpl
import cam.et.dashcamsystem.app.viewmodel.SignInViewModel
import cam.et.dashcamsystem.app.viewmodel.SignUpViewModel
import cam.et.dashcamsystem.data.repo.LoginRepoImpl
import cam.et.dashcamsystem.data.repo.ProfileRepoImpl
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.DEFAULT
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.Dispatchers
import io.ktor.serialization.kotlinx.json.json
import kotlinx.coroutines.CoroutineScope
import kotlinx.serialization.json.Json
import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.dsl.module

fun initKoin(appDeclaration: KoinAppDeclaration = {}) =
    startKoin {
        appDeclaration()
        modules(
            viewModelModule,
            useCasesModule,
            repositoryModule,
            ktorModule,
            dispatcherModule,
            coroutineScopeModule
        )
    }



val viewModelModule: Module = module {
    factory { SignInViewModel(get(), get()) }
    factory { SignUpViewModel(get()) }
}

val useCasesModule: Module = module {
    factory { SignInUseCase(get(), get()) }
    factory { SignUpUseCase(get(), get()) }
}

val repositoryModule = module {
    single<IUserRepository> { UserRepositoryImpl(get(), get()) }
    single<ILoginRepo> {LoginRepoImpl(get() , get()) }
    single<IProfileRepo> { ProfileRepoImpl(get(), get()) }
}

val dispatcherModule = module {
    factory { Dispatchers.Default }
}

val coroutineScopeModule = module {
    factory { CoroutineScope(Dispatchers.Default) }
}

val ktorModule = module {
    single {
        HttpClient {
            install(ContentNegotiation) {
                json(
                    Json {
                        ignoreUnknownKeys = true
                        prettyPrint = true
                        isLenient = true
                    }
                )
            }
            install(Logging) {
                logger = Logger.DEFAULT
                level = LogLevel.HEADERS
                filter { request ->
                    request.url.host.contains("ktor.io")
                }
                sanitizeHeader { header ->
                    header == HttpHeaders.Authorization
                }
            }
        }
    }

    single { "http://localhost:8080" }
}

fun initKoin(module: Module) {
    val koinApp = initKoin {}
    koinApp.modules(module)
}