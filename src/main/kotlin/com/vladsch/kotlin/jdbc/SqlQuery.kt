package com.vladsch.kotlin.jdbc

import java.sql.PreparedStatement

open class SqlQuery(
    val statement: String,
    params: List<Any?> = listOf(),
    inputParams: Map<String, Any?> = mapOf()
) {

    val params = ArrayList(params)
    val inputParams = HashMap(inputParams)
    val replacementMap: Map<String, List<Int>> = extractNamedParamsIndexed(statement)
    val cleanStatement: String = replaceNamedParams(statement)

    private fun extractNamedParamsIndexed(stmt: String): Map<String, List<Int>> {
        // TODO: add comment scanning as optional step to not penalize queries that cannot have comments
        return regex.findAll(stmt).filter { group ->
            val pos = stmt.lastIndexOf('\n', group.range.first)
            val lineStart = if (pos == -1) 0 else pos + 1;
            !regexSqlComment.containsMatchIn(stmt.subSequence(lineStart, stmt.length))
        }.mapIndexed { index, group ->
            Pair(group, index)
        }.groupBy({ it.first.value.substring(1) }, { it.second })
    }

    private fun replaceNamedParams(stmt: String): String {
        return regex.replace(stmt, "?")
    }

    fun populateParams(stmt: PreparedStatement) {
        if (replacementMap.isNotEmpty()) {
            replacementMap.forEach { paramName, occurrences ->
                occurrences.forEach {
                    stmt.setTypedParam(it + 1, inputParams[paramName].param())
                }
            }
        } else {
            params.forEachIndexed { index, value ->
                stmt.setTypedParam(index + 1, value.param())
            }
        }
    }

    open fun params(vararg params: Any?): SqlQuery {
        return paramsArray(params)
    }

    open fun paramsArray(params: Array<out Any?>): SqlQuery {
        this.params.addAll(params)
        return this
    }

    open fun paramsList(params: Collection<Any?>): SqlQuery {
        this.params.addAll(params)
        return this
    }

    open fun inParams(params: Map<String, Any?>): SqlQuery {
        inputParams.putAll(params)
        return this
    }

    open fun inParams(vararg params: Pair<String, Any?>): SqlQuery {
        inputParams.putAll(params)
        return this
    }

    override fun toString(): String {
        return "SqlQuery(statement='$statement', params=$params, inputParams=$inputParams, replacementMap=$replacementMap, cleanStatement='$cleanStatement')"
    }

    companion object {
        private val regex = Regex(""":\w+""")
        private val regexSqlComment = Regex("""^\s*(?:--\s|#)""")
    }
}
