package com.quicksettle.domain.model

data class ParticipantSplit(
    val name: String,
    val amountOwed: Double,
    val isPaid: Boolean = false,
)
