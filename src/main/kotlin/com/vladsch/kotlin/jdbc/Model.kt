package com.vladsch.kotlin.jdbc

import javax.json.JsonObject
import kotlin.reflect.KProperty

@Suppress("MemberVisibilityCanBePrivate")
abstract class Model<T : Model<T>>(val sqlTable: String, dbCase: Boolean, allowSetAuto: Boolean = true) {

    protected val db: ModelPropertyProvider<T> get() = _model

    internal val _model = ModelProperties<T>(this::class.simpleName ?: "<unknown>", dbCase, allowSetAuto)

    fun load(rs: Row): T {
        _model.load(rs)
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    fun load(json: JsonObject): T {
        _model.load(json)
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    fun load(other: Model<*>): T {
        _model.load(other)
        @Suppress("UNCHECKED_CAST")
        return this as T
    }

    fun toJson() = _model.toJsonObject()

    val insertQuery: SqlQuery get() = _model.sqlInsertQuery(sqlTable)
    val deleteQuery: SqlQuery get() = _model.sqlDeleteQuery(sqlTable)
    val updateQuery: SqlQuery get() = _model.sqlUpdateQuery(sqlTable)
    val selectQuery: SqlQuery get() = _model.sqlSelectQuery(sqlTable)
    val selectSql: String get() = _model.sqlSelectTable(sqlTable)

    fun insert(session: Session) {
        session.updateGetKeys(insertQuery) {
            _model.loadKeys(it)
        }
    }

    fun insertIgnoreKeys(session: Session) {
        session.update(insertQuery)
    }

    fun select(session: Session) {
        session.first(selectQuery) {
            _model.load(it)
        }
    }

    fun insertReload(session: Session) {
        insert(session)
        select(session)
    }

    fun clearAutoKeys() {
        _model.clearAutoKeys()
    }

    fun delete(session: Session) {
        session.execute(deleteQuery)
        clearAutoKeys()
    }

    fun update(session: Session) {
        if (session.execute(updateQuery)) {
            snapshot()
        }
    }

    fun updateReload(session: Session) {
        update(session)
        select(session)
    }

    fun deleteKeepAutoKeys(session: Session) {
        delete(session)
    }

    /**
     * set property value directly in the _model property map, by passing _model properties
     *
     * Can be used to set non-writable properties
     */
    protected fun <V> setProperty(prop: KProperty<*>, value: V) {
        @Suppress("UNCHECKED_CAST")
        _model.setProperty(this as T, prop, value)
    }

    fun snapshot() {
        _model.snapshot()
    }

    fun isDirty(): Boolean {
        return _model.isModified()
    }

    fun isDirty(property: KProperty<*>): Boolean {
        return _model.isModified(property)
    }

    fun appendKeys(appendable: Appendable, params: ArrayList<Any?>, delimiter: String = " AND ", sep: String = ""): String {
        return _model.appendKeys(appendable, params, delimiter, sep)
    }

    fun forEachKey(consumer: (prop: KProperty<*>, propType: PropertyType, value: Any?) -> Unit) {
        _model.forEachKey(consumer)
    }

    fun forEachProp(consumer: (prop: KProperty<*>, propType: PropertyType, value: Any?) -> Unit) {
        _model.forEachProp(consumer)
    }

    fun forEachKey(consumer: (prop: KProperty<*>, propType: PropertyType, columnName: String, value: Any?) -> Unit) {
        _model.forEachKey(consumer)
    }

    fun forEachProp(consumer: (prop: KProperty<*>, propType: PropertyType, columnName: String, value: Any?) -> Unit) {
        _model.forEachProp(consumer)
    }

    override fun toString(): String {
        val sb = StringBuilder()
        var sep = ""
        sb.append(_model.name).append("(")
        forEachProp { prop, propType, value ->
            if (value !== Unit) {
                sb.append(sep)
                if (propType.isKey) sb.append('*')
                sb.append(prop.name).append("=").append(value)
                sep = ", "
            }
        }
        sb.append(")")
        return sb.toString()
    }

    companion object {
        fun listQuery(tableName:String, params: Array<out Pair<String, Any?>>): SqlQuery {
            val conditions = HashMap<String, Any?>()
            conditions.putAll(params)
            return listQuery(tableName, conditions)
        }

        fun listQuery(tableName:String, conditions: Map<String, Any?>): SqlQuery {
            return if (!conditions.isEmpty()) {
                sqlQuery("SELECT * FROM $tableName WHERE " + conditions.keys.joinToString(" AND ") { key -> "$key = ?" })
                    .paramsList(conditions.values)
            } else {
                sqlQuery("SELECT * FROM $tableName")
            }
        }
    }
}
