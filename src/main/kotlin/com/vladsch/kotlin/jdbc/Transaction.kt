package com.vladsch.kotlin.jdbc

import java.sql.Savepoint

class Transaction(
    connection: Connection,
    autoGeneratedKeys: List<String> = listOf()
) : Session(connection, autoGeneratedKeys) {

    fun commit() {
        connection.commit()
    }

    fun begin() {
        connection.begin()
    }

    fun rollback() {
        connection.rollback()
    }

    fun setSavepoint(): Savepoint {
        return connection.setSavepoint()
    }

    fun setSavepoint(name: String): Savepoint {
        return connection.setSavepoint(name)
    }

    fun rollback(savepoint: Savepoint) {
        connection.rollback(savepoint)
    }

    fun releaseSavepoint(savepoint: Savepoint) {
        connection.releaseSavepoint(savepoint)
    }
}
