package dk.joachim.shopping.data.network

object NetworkConfig {
    /**
     * Base URL of your PHP server API (must end with /).
     *   Android emulator → local machine : "http://10.0.2.2/shopping/api/"
     *   Real device on same Wi-Fi network: "http://192.168.x.x/shopping/api/"
     *   Production server                : "https://yourserver.com/shopping/api/"
     */
    const val BASE_URL = "https://joachim.dk/shopping/"
}
