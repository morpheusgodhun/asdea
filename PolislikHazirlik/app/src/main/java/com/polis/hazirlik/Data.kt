package com.polis.hazirlik

import android.content.Context
import androidx.room.*
import kotlinx.coroutines.flow.Flow

// ---------------- Varlıklar ----------------

@Entity(tableName = "categories")
data class Category(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String
)

@Entity(
    tableName = "tests",
    foreignKeys = [ForeignKey(Category::class, ["id"], ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId")]
)
data class QuizTest(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val categoryId: Long,
    val name: String,
    val createdAt: Long
)

@Entity(
    tableName = "questions",
    foreignKeys = [ForeignKey(QuizTest::class, ["id"], ["testId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("testId")]
)
data class Question(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: Long,
    val text: String,
    val optA: String,
    val optB: String,
    val optC: String,
    val optD: String,
    val optE: String = "",
    val correct: String,
    val explanation: String = "",
    val position: Int,
    val isFavorite: Boolean = false
)

/** Bir çözüm oturumu. Test/kategori silinse bile korunması için adlar kopyalanır; FK yoktur. */
@Entity(tableName = "attempts")
data class Attempt(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val testId: Long?,
    val testName: String,
    val categoryName: String,
    val startedAt: Long,
    val finishedAt: Long? = null,
    val currentIndex: Int = 0
)

/** Çözüm başladığında soruların DEĞİŞMEZ kopyası + verilen cevap. */
@Entity(
    tableName = "attempt_questions",
    foreignKeys = [ForeignKey(Attempt::class, ["id"], ["attemptId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("attemptId")]
)
data class AttemptQuestion(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val attemptId: Long,
    val position: Int,
    val originalQuestionId: Long?,
    val text: String,
    val optA: String,
    val optB: String,
    val optC: String,
    val optD: String,
    val optE: String,
    val correct: String,
    val explanation: String,
    val selected: String? = null
)

// ---------------- Sorgu sonuç sınıfları ----------------

data class CategoryInfo(val id: Long, val name: String, val testCount: Int, val questionCount: Int)

data class TestInfo(
    val id: Long, val categoryId: Long, val name: String, val createdAt: Long,
    val categoryName: String, val questionCount: Int
)

data class AttemptSummary(
    val id: Long, val testId: Long?, val testName: String, val categoryName: String,
    val startedAt: Long, val finishedAt: Long?, val currentIndex: Int,
    val total: Int, val correct: Int, val wrong: Int
) {
    val blank: Int get() = total - correct - wrong
    val answered: Int get() = correct + wrong
    val percent: Double get() = if (total == 0) 0.0 else correct * 100.0 / total
}

data class FavoriteInfo(val id: Long, val text: String, val categoryName: String, val testName: String)

private const val TEST_INFO =
    "SELECT t.id, t.categoryId, t.name, t.createdAt, c.name AS categoryName, " +
    "(SELECT COUNT(*) FROM questions q WHERE q.testId = t.id) AS questionCount " +
    "FROM tests t JOIN categories c ON c.id = t.categoryId"

private const val ATTEMPT_SUMMARY =
    "SELECT a.id, a.testId, a.testName, a.categoryName, a.startedAt, a.finishedAt, a.currentIndex, " +
    "(SELECT COUNT(*) FROM attempt_questions q WHERE q.attemptId = a.id) AS total, " +
    "(SELECT COUNT(*) FROM attempt_questions q WHERE q.attemptId = a.id AND q.selected = q.correct) AS correct, " +
    "(SELECT COUNT(*) FROM attempt_questions q WHERE q.attemptId = a.id AND q.selected IS NOT NULL AND q.selected <> q.correct) AS wrong " +
    "FROM attempts a"

// ---------------- DAO ----------------

@Dao
abstract class AppDao {

    // Kategoriler
    @Query(
        "SELECT c.id, c.name, " +
        "(SELECT COUNT(*) FROM tests t WHERE t.categoryId = c.id) AS testCount, " +
        "(SELECT COUNT(*) FROM questions q JOIN tests t ON q.testId = t.id WHERE t.categoryId = c.id) AS questionCount " +
        "FROM categories c ORDER BY c.name COLLATE NOCASE"
    )
    abstract fun categories(): Flow<List<CategoryInfo>>

    @Query("SELECT * FROM categories WHERE id = :id")
    abstract fun category(id: Long): Flow<Category?>

    @Insert abstract suspend fun insertCategory(c: Category): Long

    @Query("UPDATE categories SET name = :name WHERE id = :id")
    abstract suspend fun renameCategory(id: Long, name: String)

    @Query("DELETE FROM categories WHERE id = :id")
    abstract suspend fun deleteCategory(id: Long)

    @Query("DELETE FROM attempts WHERE finishedAt IS NULL AND testId IN (SELECT id FROM tests WHERE categoryId = :categoryId)")
    abstract suspend fun deleteUnfinishedForCategory(categoryId: Long)

    @Transaction
    open suspend fun deleteCategoryFully(id: Long) {
        deleteUnfinishedForCategory(id)
        deleteCategory(id)
    }

    // Testler
    @Query("$TEST_INFO ORDER BY t.createdAt DESC")
    abstract fun allTests(): Flow<List<TestInfo>>

    @Query("$TEST_INFO WHERE t.categoryId = :categoryId ORDER BY t.createdAt DESC")
    abstract fun testsIn(categoryId: Long): Flow<List<TestInfo>>

    @Query("$TEST_INFO WHERE t.id = :id")
    abstract fun testInfo(id: Long): Flow<TestInfo?>

    @Query("$TEST_INFO WHERE t.id = :id")
    abstract suspend fun testInfoOnce(id: Long): TestInfo?

    @Insert abstract suspend fun insertTest(t: QuizTest): Long

    @Query("UPDATE tests SET name = :name WHERE id = :id")
    abstract suspend fun renameTest(id: Long, name: String)

    @Query("DELETE FROM tests WHERE id = :id")
    abstract suspend fun deleteTest(id: Long)

    @Query("DELETE FROM attempts WHERE testId = :testId AND finishedAt IS NULL")
    abstract suspend fun deleteUnfinishedForTest(testId: Long)

    @Transaction
    open suspend fun deleteTestFully(id: Long) {
        deleteUnfinishedForTest(id)
        deleteTest(id)
    }

    // Sorular
    @Query("SELECT * FROM questions WHERE testId = :testId ORDER BY position, id")
    abstract fun questions(testId: Long): Flow<List<Question>>

    @Query("SELECT * FROM questions WHERE testId = :testId ORDER BY position, id")
    abstract suspend fun questionsOnce(testId: Long): List<Question>

    @Query("SELECT * FROM questions WHERE id = :id")
    abstract suspend fun getQuestion(id: Long): Question?

    @Query("SELECT * FROM questions WHERE id = :id")
    abstract fun questionFlow(id: Long): Flow<Question?>

    @Query("SELECT COALESCE(MAX(position), 0) + 1 FROM questions WHERE testId = :testId")
    abstract suspend fun nextPosition(testId: Long): Int

    @Insert abstract suspend fun insertQuestion(q: Question): Long
    @Update abstract suspend fun updateQuestion(q: Question)

    @Query("DELETE FROM questions WHERE id = :id")
    abstract suspend fun deleteQuestion(id: Long)

    @Query("UPDATE questions SET position = :position WHERE id = :id")
    abstract suspend fun setPosition(id: Long, position: Int)

    @Transaction
    open suspend fun swapPositions(a: Question, b: Question) {
        setPosition(a.id, b.position)
        setPosition(b.id, a.position)
    }

    // Favoriler
    @Query("UPDATE questions SET isFavorite = :fav WHERE id = :id")
    abstract suspend fun setFavorite(id: Long, fav: Boolean)

    @Query("SELECT id FROM questions WHERE isFavorite = 1")
    abstract fun favoriteIds(): Flow<List<Long>>

    @Query(
        "SELECT q.id, q.text, c.name AS categoryName, t.name AS testName FROM questions q " +
        "JOIN tests t ON t.id = q.testId JOIN categories c ON c.id = t.categoryId " +
        "WHERE q.isFavorite = 1 ORDER BY q.id DESC"
    )
    abstract fun favorites(): Flow<List<FavoriteInfo>>

    // Çözümler
    @Insert abstract suspend fun insertAttempt(a: Attempt): Long
    @Insert abstract suspend fun insertAttemptQuestions(list: List<AttemptQuestion>)

    /** Yeni çözüm başlatır: soruların kopyasını alır. Soru yoksa 0 döner. */
    @Transaction
    open suspend fun startAttempt(testId: Long): Long {
        val info = testInfoOnce(testId) ?: return 0L
        val qs = questionsOnce(testId)
        if (qs.isEmpty()) return 0L
        deleteUnfinishedForTest(testId) // yalnızca yarım kalan eski oturum; tamamlananlara dokunulmaz
        val id = insertAttempt(
            Attempt(testId = testId, testName = info.name, categoryName = info.categoryName,
                startedAt = System.currentTimeMillis())
        )
        insertAttemptQuestions(qs.mapIndexed { i, q ->
            AttemptQuestion(
                attemptId = id, position = i, originalQuestionId = q.id, text = q.text,
                optA = q.optA, optB = q.optB, optC = q.optC, optD = q.optD, optE = q.optE,
                correct = q.correct, explanation = q.explanation
            )
        })
        return id
    }

    @Query("SELECT * FROM attempts WHERE id = :id")
    abstract suspend fun getAttempt(id: Long): Attempt?

    @Query("SELECT * FROM attempts WHERE id = :id")
    abstract fun attemptFlow(id: Long): Flow<Attempt?>

    @Query("SELECT * FROM attempt_questions WHERE attemptId = :attemptId ORDER BY position")
    abstract fun attemptQuestions(attemptId: Long): Flow<List<AttemptQuestion>>

    @Query("UPDATE attempt_questions SET selected = :selected WHERE id = :id")
    abstract suspend fun setSelected(id: Long, selected: String)

    @Query("UPDATE attempts SET currentIndex = :index WHERE id = :id")
    abstract suspend fun setIndex(id: Long, index: Int)

    @Query("UPDATE attempts SET finishedAt = :time WHERE id = :id")
    abstract suspend fun finishAttempt(id: Long, time: Long)

    @Query("$ATTEMPT_SUMMARY WHERE a.id = :id")
    abstract fun attemptSummary(id: Long): Flow<AttemptSummary?>

    @Query("$ATTEMPT_SUMMARY WHERE a.finishedAt IS NOT NULL ORDER BY a.finishedAt DESC")
    abstract fun finishedSummaries(): Flow<List<AttemptSummary>>

    @Query("$ATTEMPT_SUMMARY WHERE a.testId = :testId AND a.finishedAt IS NOT NULL ORDER BY a.finishedAt DESC")
    abstract fun finishedSummariesForTest(testId: Long): Flow<List<AttemptSummary>>

    @Query("$ATTEMPT_SUMMARY WHERE a.testId = :testId AND a.finishedAt IS NULL ORDER BY a.startedAt DESC LIMIT 1")
    abstract fun unfinishedSummary(testId: Long): Flow<AttemptSummary?>
}

@Database(
    entities = [Category::class, QuizTest::class, Question::class, Attempt::class, AttemptQuestion::class],
    version = 1,
    exportSchema = false
)
abstract class AppDb : RoomDatabase() {
    abstract fun dao(): AppDao

    companion object {
        // Bilerek fallbackToDestructiveMigration KULLANILMIYOR: şema değişirse veri silinmez,
        // gelecekte gerekirse düzgün Migration yazılır.
        fun build(ctx: Context): AppDb =
            Room.databaseBuilder(ctx.applicationContext, AppDb::class.java, "polislik.db").build()
    }
}
