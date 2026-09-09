package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Database(entities = [DrspDailyLog::class], version = 1, exportSchema = false)
abstract class PmddDatabase : RoomDatabase() {
    abstract fun drspDao(): DrspDao

    companion object {
        @Volatile
        private var INSTANCE: PmddDatabase? = null

        fun getDatabase(context: Context): PmddDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PmddDatabase::class.java,
                    "homavales_pmdd_db"
                )
                .fallbackToDestructiveMigration(false)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            populateSampleData(INSTANCE?.drspDao())
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateSampleData(dao: DrspDao?) {
            if (dao == null) return
            val sampleLogs = mutableListOf<DrspDailyLog>()
            val today = LocalDate.now()
            val formatter = DateTimeFormatter.ISO_LOCAL_DATE

            // Generate 30 days of continuous cycle history
            for (i in 29 downTo 0) {
                val date = today.minusDays(i.toLong())
                val dayOfCycle = ((29 - i + 24) % 28) + 1
                val isBleed = dayOfCycle in 1..4
                val isLateLutealPhase = dayOfCycle in 21..28

                val drspBase = if (isLateLutealPhase) 4 else if (isBleed) 2 else 1
                val rhrBase = if (isLateLutealPhase) 76 + (i % 3) else 64 + (i % 3)
                val hrvBase = if (isLateLutealPhase) 34.0 + (i % 4) else 52.0 + (i % 5)
                val glucoseBase = if (isLateLutealPhase) 112.0 + (i % 6) else 94.0 + (i % 4)
                val cvBase = if (isLateLutealPhase) 24.5 else 15.2

                sampleLogs.add(
                    DrspDailyLog(
                        date = date.format(formatter),
                        cycleDay = dayOfCycle,
                        isBleeding = isBleed,
                        bleedingFlow = if (isBleed) "Medium" else "None",
                        depressedMood = drspBase,
                        anxietyTension = drspBase,
                        moodSwings = if (isLateLutealPhase) 5 else 1,
                        angerIrritability = if (isLateLutealPhase) 5 else 1,
                        lethargyFatigue = if (isLateLutealPhase) 5 else 2,
                        physicalBreastTenderness = if (isLateLutealPhase) 4 else 1,
                        physicalBloatingWeightGain = if (isLateLutealPhase) 4 else 1,
                        restingHeartRate = rhrBase,
                        hrvRmssd = hrvBase,
                        sleepDurationHours = if (isLateLutealPhase) 6.2 else 7.8,
                        cgmCurrentGlucose = glucoseBase,
                        cgmMeanDailyGlucose = glucoseBase + 4.0,
                        cgmGlucoseVariabilityCv = cvBase
                    )
                )
            }
            dao.insertAll(sampleLogs)
        }
    }
}
