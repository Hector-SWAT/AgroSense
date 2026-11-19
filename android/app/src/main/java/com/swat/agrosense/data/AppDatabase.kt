package com.swat.agrosense.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AgroDao {
    // Clientes
    @Query("SELECT * FROM clients ORDER BY name COLLATE NOCASE") fun clients(): Flow<List<Client>>
    @Query("SELECT * FROM clients") suspend fun clientsOnce(): List<Client>
    @Insert suspend fun insertClient(c: Client): Long
    @Update suspend fun updateClient(c: Client)
    @Delete suspend fun deleteClient(c: Client)

    // Parcelas
    @Query("SELECT * FROM plots ORDER BY name COLLATE NOCASE") fun plots(): Flow<List<Plot>>
    @Query("SELECT * FROM plots") suspend fun plotsOnce(): List<Plot>
    @Insert suspend fun insertPlot(p: Plot): Long
    @Update suspend fun updatePlot(p: Plot)
    @Delete suspend fun deletePlot(p: Plot)

    // Productos
    @Query("SELECT * FROM products ORDER BY name COLLATE NOCASE, crop") fun products(): Flow<List<Product>>
    @Query("SELECT * FROM products") suspend fun productsOnce(): List<Product>
    @Query("SELECT COUNT(*) FROM products") suspend fun productCount(): Int
    @Insert suspend fun insertProducts(p: List<Product>)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun upsertProduct(p: Product): Long
    @Delete suspend fun deleteProduct(p: Product)

    // Aplicaciones
    @Query("SELECT * FROM applications ORDER BY date DESC, id DESC") fun applications(): Flow<List<ApplicationRecord>>
    @Query("SELECT * FROM applications") suspend fun applicationsOnce(): List<ApplicationRecord>
    @Insert suspend fun insertApplication(a: ApplicationRecord): Long
    @Delete suspend fun deleteApplication(a: ApplicationRecord)

    @Query("DELETE FROM applications") suspend fun clearApplications()
    @Query("DELETE FROM plots") suspend fun clearPlots()
    @Query("DELETE FROM clients") suspend fun clearClients()
    @Query("DELETE FROM products") suspend fun clearProducts()
    @Insert suspend fun insertClients(c: List<Client>)
    @Insert suspend fun insertPlots(p: List<Plot>)
    @Insert suspend fun insertApplications(a: List<ApplicationRecord>)
}

@Database(entities = [Client::class, Plot::class, Product::class, ApplicationRecord::class], version = 1, exportSchema = true)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dao(): AgroDao

    companion object {
        @Volatile private var instance: AppDatabase? = null
        fun get(context: Context): AppDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "agrosense.db")
                .build().also { instance = it }
        }

        /** Productos iniciales tomados de Datos_Completos.xlsx (dosis tal como aparecen en la ficha). */
        fun seedProducts(): List<Product> = Catalog.labelSeeds.map {
            Product(
                name = it.name, active = it.active, category = it.category, crop = it.crop,
                target = it.target, scientific = it.scientific, doseMin = it.doseMin, doseMax = it.doseMax,
                unit = it.unit, method = it.note, source = "Catálogo",
                priceUnit = if (it.unit.startsWith("g") || it.unit.startsWith("kg")) "kg" else "L"
            )
        }
    }
}
