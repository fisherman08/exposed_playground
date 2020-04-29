package com.github.fisherman08.exposed_playground

import org.jetbrains.exposed.dao.id.UUIDTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.TransactionManager
import org.jetbrains.exposed.sql.transactions.transaction
import java.sql.Connection


val DATA_FILE_PATH = "./db/test.db"

fun main(args: Array<String>) {
    Database.connect("jdbc:sqlite:${DATA_FILE_PATH}", "org.sqlite.JDBC")
    TransactionManager.manager.defaultIsolationLevel = Connection.TRANSACTION_SERIALIZABLE

    transaction {
        addLogger(StdOutSqlLogger)
        SchemaUtils.drop(Players, Teams)
        SchemaUtils.create(Players, Teams)

        val angelsId = Teams.insert {
            it[name] = "LA Angels"
        } get Teams.id



        val addedId = Players.insert {
            it[name] = "Shohei Ohtani"
            it[teamId] = angelsId.value
        } get Players.id

        Players.selectAll().forEach { player ->
            println(player[Players.name])
        }

        //(Players innerJoin Teams).slice(Players.name, Teams.name).selectAll().forEach { row ->
        (Players.join(
            joinType = JoinType.LEFT,
            onColumn = Players.teamId,
            otherTable = Teams,
            otherColumn = Teams.id
        )).slice(Players.name, Teams.name)
            .select {
                Players.name.isNotNull()
            }
            .orderBy(column = Players.name, order = SortOrder.ASC)
            .groupBy(Players.id)
            .forEach { row ->
                println("${row[Players.name]}: ${row[Teams.name]}")
            }

    }
}

object Players: UUIDTable() {
    val name = text("name")
    val teamId = uuid("team_id").references(Teams.id).nullable()
}

object Teams: UUIDTable() {
    val name = text("name")
}
