package dk.joachim.shopping

import android.app.Application
import dk.joachim.shopping.data.RecipeStepTimer
import dk.joachim.shopping.data.ShoppingRepository

class ShoppingApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        ShoppingRepository.init(this)
        RecipeStepTimer.init(this)
    }
}
