    override fun onCreate(savedInstanceState: Bundle?) {
        val prefs = getSharedPreferences("kryptos_prefs", Context.MODE_PRIVATE)
        val isDark = prefs.getBoolean("dark_mode", true)
        if (isDark) AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        else AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        // Check for updates only once every 24 hours
        val lastCheck = prefs.getLong("last_update_check", 0)
        val currentTime = System.currentTimeMillis()
        val oneDay = 24 * 60 * 60 * 1000 // 24 hours in milliseconds

        if (currentTime - lastCheck > oneDay) {
            checkForUpdates()
            prefs.edit().putLong("last_update_check", currentTime).apply()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            finish()
        }, 1500)
    }
